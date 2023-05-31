(ns me.noonian.property-graph
  (:require [clojure.set :as set]
            [clojure.tools.logging :as log]))

(def ^:dynamic *validate-edge-refs* nil)

(defrecord Node [id props])
(defrecord DirectedEdge [id label from to props])
(defrecord PropertyGraph [])

(defn node? [x] (instance? Node x))
(defn edge? [x] (instance? DirectedEdge x))
(defn ent? [x] (or (node? x) (edge? x)))
(defn graph? [x] (instance? PropertyGraph x))

(declare id by-id)

(defn ->id [ent-or-id]
  (if (ent? ent-or-id)
    (id ent-or-id)
    ent-or-id))

(defn ->ent [g ent-or-id]
  (if (ent? ent-or-id)
    ent-or-id
    (by-id g ent-or-id)))

(defn id [entity] (:id entity))

(defn by-id [g id]
  (get-in g [:id->ent (->id id)]))

(defn props [g entity]
  (:props (by-id g (->id entity))))

(defn nodes [g]
  (->> (:nodes g)
       (map (partial by-id g))
       (into #{})))

(defn edges [g]
  (->> (:edges g)
       (map (partial by-id g))
       (into #{})))

(defn entities [g]
  (set/union (nodes g) (edges g)))

(defn outgoing
  ([g node] (outgoing g node identity))
  ([g node pred]
   (->> (get (:node->outgoing g) (->id node) #{})
        (map (partial by-id g))
        (filter pred)
        (into #{}))))

(defn incoming
  ([g node] (incoming g node identity))
  ([g node pred]
   (->> (get (:node->incoming g) (->id node) #{})
        (map (partial by-id g))
        (filter pred)
        (into #{}))))

(defn node-edges
  "Return all edges relating to `node` in `g` (incoming and outcoming)."
  [g node]
  (set/union (outgoing g node)
             (incoming g node)))

(defn from [edge] (:from edge))
(defn to [edge] (:to edge))
(defn label [edge] (:label edge))

(defn contains-ent? [g ent]
  (let [id (->id ent)]
    (contains? (:id->ent g) id)))

(contains? {:fo "bar"} :foo)

(defn new-graph []
  (merge (PropertyGraph.)
         {:nodes #{}
          :edges #{}
          :id->ent {}
          :node->outgoing {}
          :node->incoming {}}))

(defn node
  ([] (node {}))
  ([props]
   (->Node (random-uuid) props)))

(defn directed-edge
  ([a label b] (directed-edge a label b {}))
  ([a label b props]
   (map->DirectedEdge
     {:id (random-uuid)
      :label label
      :from (->id a)
      :to (->id b)
      :props props})))

(defn add-node [g {:keys [id] :as node}]
  (log/debugf "Adding node %s" id)
  (if-not (and g node)
    g
    (-> g
        (update :nodes conj id)
        (assoc-in [:id->ent id] node))))

(defn valid-edge? [g edge]
  (and (by-id g (from edge))
       (by-id g (to edge))))

(defn add-edge [g {:keys [id from to label] :as edge}]
  (log/debugf "Adding edge %s - %s [%s] %s" id from label to)
  (when *validate-edge-refs*
    (when-not (and (by-id g from)
                   (by-id g to))
      (throw (ex-info "Atttempted to add edge referencing missing nodes" {:edge edge}))))
  (if-not (and g edge)
    g
    (-> g
        (update :edges conj id)
        (assoc-in [:id->ent id] edge)
        (update-in [:node->outgoing from] (fnil conj #{}) id)
        (update-in [:node->incoming to] (fnil conj #{}) id))))

(defn add-nodes [g nodes]
  (reduce add-node g nodes))

(defn add-edges [g edges]
  (reduce add-edge g edges))

(defn remove-node
  "Remove node from g. Removes any edges that reference node."
  [g node]
  (if-not (and g node)
    g
    (let [id (->id node)
          edges-to-remove (map ->id (node-edges g node))]
      (log/debugf "Removing node %s and %s edges that reference it" id (count edges-to-remove))
      (-> g
          (update :nodes disj id)
          (update :id->ent dissoc id)
          (update :edges set/difference edges-to-remove)))))

(defn remove-edge
  "Remove edge from g."
  [g edge]
  (if-not (and g edge)
    g
    (let [id (->id edge)]
      (log/debugf "Removing %s" id)
      (-> g
          (update :edges disj id)
          (update :id->ent dissoc id)))))

(defn update-props [g ent f & args]
  (apply update-in g [:id->ent (->id ent) :props] f args))

(defn find-ents [g f]
  (into #{} (filter f (entities g))))

(defmethod print-method PropertyGraph [g writer]
  (let [s (str "PropertyGraph<nodes=" (count (nodes g)) ", edges=" (count (edges g)) ">")]
    (.write writer s)))

(defmethod print-method Node [n writer]
  (let [s (str "Node<" (id n) ">")]
    (.write writer s)))

(defmethod print-method DirectedEdge [e writer]
  (let [s (str "Edge<from=" (:from e) ", label=" (:label e) ", to=" (:to e) ">")]
    (.write writer s)))
