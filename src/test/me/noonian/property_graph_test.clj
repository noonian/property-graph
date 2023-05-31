(ns me.noonian.property-graph-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.pprint :refer [pprint]]
            [me.noonian.property-graph :as pg]
            [me.noonian.property-graph.dot :as dot]))

(def fred (pg/node {:name "Fred"}))
(def ethel (pg/node {:name "Ethel"}))
(def lucy (pg/node {:name "Lucy"}))
(def fred->lucy (pg/directed-edge fred :friends-with lucy))
(def ethel->fred (pg/directed-edge ethel :friends-with fred))
(def lucy->ethel (pg/directed-edge lucy :friends-with ethel))

(def clojure-friends
  (-> (pg/new-graph)
      (pg/add-nodes [fred ethel lucy])
      (pg/add-edges [fred->lucy ethel->fred lucy->ethel])))

(deftest by-id
  (testing "Returns ent given an ent id or ent"
    (is (nil? (pg/by-id (pg/new-graph))))
    (is (= fred
           (pg/by-id (pg/add-node (pg/new-graph) fred))
           (pg/by-id clojure-friends (:id fred)))))
  (testing "Returns nil for nil graphs and missing keys"
    (is (nil? (pg/by-id nil nil)))
    (is (nil? (pg/by-id nil (:id fred))))
    (is (nil? (pg/by-id clojure-friends nil)))
    (is (nil? (pg/by-id clojure-friends (random-uuid))))
    (is (nil? (pg/by-id clojure-friends :nonexistent-key)))))

(deftest props
  (testing "Returns nil for nil graphs and nonexistent ents"
    (is (nil? (pg/props nil nil)))
    (is (nil? (pg/props nil :nonexistent-key)))
    (is (nil? (pg/props clojure-friends :nonexistent-key)))
    (is (nil? (pg/props clojure-friends nil))))
  (let [n0 (pg/node)
        n1 (pg/node {:with "props"})
        e0 (pg/directed-edge n0 :knows n1)
        e1 (pg/directed-edge n0 :dislikes n1 {:reason "n1 is a bully"})
        g (-> (pg/new-graph)
              (pg/add-nodes [n0 n1])
              (pg/add-edges [e0 e1]))]
    (testing "Returns props for nodes in graph given ent or id"
      (is (= {}
             (pg/props g n0)
             (pg/props g (:id n0))))
      (is (= {:with "props"}
             (pg/props g n1)
             (pg/props g (:id n1)))))
    (testing "Returns props for edges in graph given ent or id"
      (is (= {}
             (pg/props g e0)
             (pg/props g (:id e0))))
      (is (= {:reason "n1 is a bully"}
             (pg/props g e1)
             (pg/props g (:id e1)))))))

(deftest add-node
  (testing "Happy path"
    (is (= fred (pg/by-id (pg/add-node (pg/new-graph) fred) (:id fred)))))
  (testing "Does not pollute graph with nil nodes"
    (let [g (pg/add-node (pg/new-graph) nil)]
      (is (not-any? nil? (pg/nodes g)))
      (is (not-any? nil? (vals (:id->ent g))))))
  (testing "Returns nil when graph is nil"
    (is (nil? (pg/add-node nil nil)))
    (is (nil? (pg/add-node nil fred)))))

(deftest add-edge
  (testing "Happy path"
    (let [g (pg/add-nodes (pg/new-graph) [fred lucy])]
      (is (= #{fred->lucy} (pg/edges (pg/add-edge g fred->lucy))))))
  (testing "Can add edges referencing non-existant nodes"
    (is (pg/graph? (pg/add-edge (pg/new-graph) fred->lucy))))
  (testing "Cannot add edges referencing non-existant nodes when *validate-edge-refs* is true"
    (is (thrown? Exception
          (binding [pg/*validate-edge-refs* true]
            (pg/add-edge (pg/new-graph) fred->lucy)))))
  (testing "Returns unmodified graph when edge is nil"
    (let [g (pg/new-graph)]
      (is (= g (pg/add-edge g nil)))))
  (testing "Returns nil when graph is nil"
    (is (nil? (pg/add-edge nil nil)))
    (is (nil? (pg/add-edge nil fred->lucy)))))

(deftest by-id
  (testing "Returns nil for nil graphs"
    (is (nil? (pg/by-id nil nil)))
    (is (nil? (pg/by-id nil (random-uuid)))))
  (testing "Happy path"
    (doseq [ent (pg/entities clojure-friends)]
      (is (= ent
             (pg/by-id clojure-friends ent)
             (pg/by-id clojure-friends (:id ent))))))
  (testing "Not-found keys return nil"
    (is (nil? (pg/by-id clojure-friends nil)))
    (is (nil? (pg/by-id clojure-friends (random-uuid))))
    (is (nil? (pg/by-id clojure-friends :nonexistent-key)))))

(deftest nodes
  (testing "Returns empty set for empty graph"
    (is (= #{} (pg/nodes (pg/new-graph)))))
  (testing "Returns all nodes in graph"
    (is (= #{fred ethel lucy}
           (pg/nodes clojure-friends))))
  (testing "Returns empty set for nil graphs"
    (is (= #{} (pg/nodes nil)))))

(deftest edges
  (testing "Returns empty set for empty graph"
    (is (= #{} (pg/edges (pg/new-graph)))))
  (testing "Returns all edges in graph"
    (is (= #{fred->lucy lucy->ethel ethel->fred}
           (pg/edges clojure-friends))))
  (testing "Returns empty set for nil graphs"
    (is (= #{} (pg/edges nil)))))

(deftest entities
  (testing "Returns empty set for empty graph"
    (is (= #{} (pg/entities (pg/new-graph)))))
  (testing "Returns union of nodes and edges"
    (is (= #{fred ethel lucy fred->lucy lucy->ethel ethel->fred}
           (pg/entities clojure-friends))))
  (testing "Returns empty set for nil graphs"
    (is (= #{} (pg/entities nil)))))

(deftest outgoing
  (testing "Returns edges whose :from is the given node"
    (is (= #{fred->lucy} (pg/outgoing clojure-friends fred)))
    (is (= #{ethel->fred} (pg/outgoing clojure-friends ethel)))
    (is (= #{lucy->ethel} (pg/outgoing clojure-friends lucy))))
  (testing "works with either id or ent"
    (is (= #{lucy->ethel}
           (pg/outgoing clojure-friends lucy)
           (pg/outgoing clojure-friends (:id lucy))))))

(deftest incoming
  (testing "Returns edges whose :to is the given node"
    (is (= #{ethel->fred} (pg/incoming clojure-friends fred)))
    (is (= #{lucy->ethel} (pg/incoming clojure-friends ethel)))
    (is (= #{fred->lucy} (pg/incoming clojure-friends lucy))))
  (testing "works with either id or ent"
    (is (= #{fred->lucy}
           (pg/incoming clojure-friends lucy)
           (pg/incoming clojure-friends (:id lucy))))))

(deftest node-edges
  (testing "Returns edges whose :to or :from is the given node"
    (is (= #{ethel->fred fred->lucy} (pg/node-edges clojure-friends fred)))
    (is (= #{lucy->ethel ethel->fred} (pg/node-edges clojure-friends ethel)))
    (is (= #{fred->lucy lucy->ethel} (pg/node-edges clojure-friends lucy)))))

(deftest remove-node
  (let [g (pg/remove-node clojure-friends fred)]
    (testing "Removes node from graph"
      (is (= fred (pg/by-id clojure-friends fred)))
      (is (nil? (pg/by-id g fred)))
      (is (= #{lucy ethel} (pg/nodes g))))
    (testing "Removes edges referencing node from graph"
      (is (= #{lucy->ethel} (pg/edges g)))))
  (testing "Handles nils and missing keys gracefully"
    (is (= clojure-friends (pg/remove-node clojure-friends nil)))
    (is (= clojure-friends (pg/remove-node clojure-friends {})))
    (is (nil? (pg/remove-node nil nil)))))

(deftest remove-edge
  (testing "Removes edge from graph"
    (let [g (pg/remove-edge clojure-friends fred->lucy)]
      (is (= fred->lucy (pg/by-id clojure-friends fred->lucy)))
      (is (nil? (pg/by-id g fred->lucy)))
      (is (= #{lucy->ethel ethel->fred} (pg/edges g)))))
  (testing "Handles nils and missing keys gracefully"
    (is (= clojure-friends (pg/remove-edge clojure-friends nil)))
    (is (= clojure-friends (pg/remove-edge clojure-friends {})))
    (is (nil? (pg/remove-edge nil nil)))))

(deftest update-props
  (testing "Returns a new graph where an entities props have been modified"
    (let [g (pg/update-props clojure-friends (:id fred) assoc :updated true)]
      (is (= {:name "Fred"}
             (pg/props clojure-friends fred)
             (pg/props clojure-friends (:id fred))))
      (is (= {:name "Fred"
              :updated true}
             (pg/props g fred)
             (pg/props g (:id fred)))))))

(deftest find-ents
  (testing "Returns ents for which `pred` returns a truthy value"
    (is (= #{fred} (pg/find-ents clojure-friends #(= "Fred" (:name (pg/props clojure-friends %))))))))
