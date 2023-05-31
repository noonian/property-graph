(ns me.noonian.property-graph.dot
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [me.noonian.property-graph :as pg]))

;; See https://graphviz.org/doc/info/lang.html

(defn escape [id]
  (pr-str (str id)))

(defn node-label [node {:keys [label-prop]}]
  (escape (or (and label-prop (get-in node [:props label-prop]))
              (pg/id node))))

(defn render-node
  [g node {:keys [label-prop] :as opts}]
  (log/debugf "Rendering node %s" (pg/id node))
  (let [label (node-label node opts)
        out (map (comp #(node-label % opts) (partial pg/by-id g) pg/to)
                 (pg/outgoing g node))]
    (str label)))

(defn render-edge [g edge {:keys [label-prop] :as opts}]
  (log/debugf "Rendering edge %s" (pg/id edge))
  (let [from-node (pg/by-id g (pg/from edge))
        to-node (pg/by-id g (pg/to edge))
        label (escape (pg/label edge))
        from-label (node-label from-node opts)
        to-label (node-label to-node opts)]
    (str from-label " -> " to-label " [label=" label "]")))

(defn graph->dot
  ([g] (graph->dot g {}))
  ([g opts]
   (str "digraph {\n"
        (str/join "\n" (map #(render-node g % opts) (pg/nodes g)))
        "\n"
        (str/join "\n" (map #(render-edge g % opts) (pg/edges g)))
        "\n}")))

(comment

  (def g
    (let [fred (pg/node {:name "Fred"})
          ethel (pg/node {:name "Ethel"})
          lucy (pg/node {:name "Lucy"})]
      (-> (pg/new-graph)
          (pg/add-nodes [fred ethel lucy])
          (pg/add-edges [(pg/directed-edge fred :friends-with lucy)
                         (pg/directed-edge ethel :friends-with fred)
                         (pg/directed-edge lucy :friends-with ethel)]))))

  (spit "clojure-friends.dot" (graph->dot g {:label-prop :name}))

  )
