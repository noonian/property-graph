(ns me.noonian.property-graph.dot-test
  (:require [clojure.test :refer [deftest is testing]]
            [me.noonian.property-graph :as pg]
            [me.noonian.property-graph.dot :as dot]))

(deftest render-node
  (testing "Node with no properties renders using (pr-str id)"
    (let [a (assoc (pg/node) :id :a)
          g (-> (pg/new-graph)
                (pg/add-node a))]
      (is (= "\":a\""
             (dot/render-node g a {})))))
  (testing "Override node label with :label-prop option"
    (let [a (pg/node {:name "Node A"})
          g (-> (pg/new-graph) (pg/add-node a))]
      (is (= "\"Node A\""
             (dot/render-node g a {:label-prop :name}))))))

(deftest render-edge
  (testing "Edge renders with label and node ids by default"
    (let [a (assoc (pg/node) :id :a)
          b (assoc (pg/node) :id :b)
          e (pg/directed-edge a :points-to b)
          g (-> (pg/new-graph)
                (pg/add-nodes [a b])
                (pg/add-edge e))]
      (is (= "\":a\" -> \":b\" [label=\":points-to\"]"
             (dot/render-edge g e {})))))
  (testing "Renders nodes using :label-prop if provided"
    (let [a (pg/node {:name "Node A"})
          b (pg/node {:name "Node B"})
          e (pg/directed-edge a :points-to b)
          g (-> (pg/new-graph)
                (pg/add-nodes [a b])
                (pg/add-edge e))]
      (is (= "\"Node A\" -> \"Node B\" [label=\":points-to\"]"
             (dot/render-edge g e {:label-prop :name}))))))

(deftest graph->dot
  (testing "Renders digraph"
    (is (= "digraph {
}"
           (dot/graph->dot (pg/new-graph)))))
  #_(testing "Renders full graph using ids as labels"
    (let [a (assoc (pg/node) :id :a)
          b (assoc (pg/node) :id :b)
          e (pg/directed-edge a :points-to b)
          g (-> (pg/new-graph)
                (pg/add-nodes [a b])
                (pg/add-edge e))]
      (is (= [ "digraph {

}
"
              "\":b\""
              "\":a\""
              "\":a\" -> \":b\" [label=\":points-to\"]"]
             (dot/graph->dot g)))))
  (testing "Renders full graph with :label-prop"
    (let [a (pg/node {:name "Node A"})
          b (pg/node {:name "Node B"})
          e (pg/directed-edge a :points-to b)
          g (-> (pg/new-graph)
                (pg/add-nodes [a b])
                (pg/add-edge e))]
      #_(is (= "digraph {
\"Node B\"
\"Node A\"
\"Node A\" -> \"Node B\" [label=\":points-to\"]
}
"
             (dot/graph->dot g {:label-prop :name}))))))
