(ns me.noonian.property-graph.query
  (:require [clojure.tools.logging :as log]
            [me.noonian.property-graph :as pg]
            [clojure.walk :as walk]))

(defn run-query [g q env]
  (log/debugf "Running query with %s" q)
  (if (or (not (seq q))
          (= 1 (count q)))
    (dissoc env '_)
    (let [[sym label] (take 2 q)
          allowed-label? (if (set? label) label #{label})]
      (let [cur-nodes (get env sym #{})
            edges (into #{}
                    (comp (mapcat (partial pg/outgoing g))
                          (filter (comp allowed-label? pg/label)))
                    cur-nodes)
            next-sym (first (drop 2 q))]
        (recur g (into [] (drop 2 q)) (assoc env next-sym (into #{} (map pg/to edges))))))))

(comment

  (def fred (pg/node {:name "Fred"}))

  (def g
    (let [ethel (pg/node {:name "Ethel"})
          lucy (pg/node {:name "Lucy"})]
      (-> (pg/new-graph)
          (pg/add-nodes [fred ethel lucy])
          (pg/add-edges [(pg/directed-edge fred :friends-with lucy)
                         (pg/directed-edge ethel :friends-with fred)
                         (pg/directed-edge lucy :friends-with ethel)]))))

  (run-query g '[root :friends-with _ :friends-with ?friends-of-friends] {'root #{(:id fred)}})

  )
