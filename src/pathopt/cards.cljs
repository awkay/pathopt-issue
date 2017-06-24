(ns pathopt.cards
  (:require
    [om.dom :as dom]
    [om.util :as util]
    [om.next :as om :refer [defui]]
    [devcards.core :as dc :refer-macros [defcard defcard-om-next]]))

(def initial-state
  {
   :child/by-id      {1 {:db/id 1 :child/value 1}}
   :d/by-id          {1 {:db/id 1 :type :d/by-id :value "D" :child [:child/by-id 1]}}
   :c/by-id          {1 {:db/id 1 :type :c/by-id :value "C"}}
   :controller       [:controller/by-id :the-one]
   :controller/by-id {:the-one {:current-tab [:c/by-id 1]}}
   })

(defui ^:once DChild
  static om/Ident
  (ident [this props] [:child/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :child/value])
  Object
  (render [this]
    (let [{:keys [child/value]} (om/props this)]
      (dom/div #js {:onClick #(om/transact! this '[(do-thing) :value])} (str value)))))

(def ui-child (om/factory DChild))

(defui ^:once D
  static om/IQuery
  (query [this] [:db/id :value :type {:child (om/get-query DChild)}])
  Object
  (render [this]
    (let [{:keys [value child]} (om/props this)]
      (dom/div nil ;#js {:onClick #(om/transact! this '[(do-thing) :b])}
        (str "IN D: " value)
        (ui-child child)))))

(def ui-d (om/factory D))

(defui ^:once C
  static om/IQuery
  (query [this] [:db/id :value :type])
  Object
  (render [this]
    (let [{:keys [value]} (om/props this)]
      (dom/div nil ;#js {:onClick #(om/transact! this `[(do-thing) :child/value])}
        (dom/button #js {:onClick #(om/transact! this '[(nav-to {:tab :d/by-id}) :controller])} "D")
        (str "IN C:" value)))))

(def ui-c (om/factory C))

(defui ^:once BUnion
  static om/Ident
  (ident [this props] [(:type props) (:db/id props)])
  static om/IQuery
  (query [this] {:c/by-id (om/get-query C) :d/by-id (om/get-query D)})
  Object
  (render [this]
    (let [{:keys [type] :as props} (om/props this)]
      (case type
        :c/by-id (ui-c props)
        :d/by-id (ui-d props)))))

(def ui-b (om/factory BUnion))

(defui UnionController
  static om/Ident
  (ident [this props] [:controller/by-id :the-one])
  static om/IQuery
  (query [this] [{:current-tab (om/get-query BUnion)}])
  Object
  (render [this]
    (let [{:keys [current-tab] :as props} (om/props this)]
      (ui-b current-tab))))

(def ui-controller (om/factory UnionController))

(defui ^:once Root
  static om/IQuery
  (query [this] [{:controller (om/get-query UnionController)}])
  Object
  (render [this]
    (let [{:keys [controller]} (om/props this)]
      (dom/div nil
        (dom/button #js {:onClick #(om/transact! this '[(nav-to {:tab :c/by-id})])} "C")
        (ui-controller controller)))))

(defn read-local
  [{:keys [query target state ast]} dkey _]
  (when (not target)
    (case dkey
      (let [top-level-prop (nil? query)
            key            (or (:key ast) dkey)
            by-ident?      (util/ident? key)
            union?         (map? query)
            data           (if by-ident? (get-in @state key) (get @state key))]
        (js/console.log "key" key "QUERY" query)
        {:value
         (cond
           union? (get (om/db->tree [{key query}] @state @state) key)
           top-level-prop data
           :else (om/db->tree query data @state))}))))

(defn mutate [{:keys [state]} k {:keys [tab]}]
  {:action (fn []
             (js/console.log :mutation k)
             (cond
               (= k 'nav-to) (swap! state assoc-in [:controller/by-id :the-one :current-tab] [tab 1])
               (= k 'do-thing) (swap! state (fn [s] (-> s
                                                      (update-in [:child/by-id 1 :child/value] inc)
                                                      (update-in [:d/by-id 1 :value] #(str % "D"))
                                                      (assoc-in [:controller/by-id :the-one :current-tab] [:c/by-id 1])
                                                      )))))})

(def parser (om/parser {:read read-local :mutate mutate}))
(defonce reconciler (om/reconciler {:state   (atom initial-state)
                                    :parser  parser
                                    :pathopt true}))

(defcard-om-next sample
  "A sample"
  Root
  reconciler)
