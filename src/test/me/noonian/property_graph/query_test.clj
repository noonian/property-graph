(ns me.noonian.property-graph.query-test
  (:require [clojure.test :refer [deftest is testing]]
            [me.noonian.property-graph :as pg]
            [me.noonian.property-graph.query :as q]))

(deftest run-query
  (let [fred (pg/node {:name "Fred"})
        ethel (pg/node {:name "Ethel"})
        lucy (pg/node {:name "Lucy"})
        g (-> (pg/new-graph)
              (pg/add-nodes [fred ethel lucy])
              (pg/add-edges [(pg/directed-edge fred :friends-with lucy)
                             (pg/directed-edge ethel :friends-with fred)
                             (pg/directed-edge lucy :friends-with ethel)
                             (pg/directed-edge lucy :dislikes fred)]))
        initial-env {'root #{(:id fred)}}]
    (testing "Follows relationships expressed in query and binds results"
      (let [q '[root :friends-with friends :friends-with friends-of-friends]]
        (is (= {'root #{(:id fred)}
                'friends #{(:id lucy)}
                'friends-of-friends #{(:id ethel)}}
               (q/run-query g q initial-env)))))
    (testing "It omits `_` in results if used in a query binding"
      (let [q '[root :friends-with _ :friends-with friends-of-friends]]
        (is (= {'root #{(:id fred)}
                'friends-of-friends #{(:id ethel)}}
               (q/run-query g q initial-env)))))
    (testing "Sets of labels can be used to traverse multiple properties at once"
      (let [q '[root :friends-with _ #{:friends-with :dislikes} friends-or-enemies-of-friends]]
        (is (= {'root #{(:id fred)}
                'friends-or-enemies-of-friends #{(:id ethel)
                                                 (:id fred)}}
               (q/run-query g q initial-env)))))))
