(ns pathopt.cards
  (:require
    [om.dom :as dom]
    [goog.dom :as gdom]
    [om.util :as util]
    [om.next :as om :refer [defui]]))

(def dropdown-table :bootstrap.dropdown/by-id)
(def dropdown-item-table :bootstrap.dropdown-item/by-id)

(defn dropdown-ident [id-or-props]
  (if (map? id-or-props)
    [(:type id-or-props) (:id id-or-props)]
    [dropdown-table id-or-props]))

(defn dropdown-item-ident [id-or-props]
  (if (map? id-or-props)
    (dropdown-item-ident (:id id-or-props))
    [dropdown-item-table id-or-props]))

(defui ^:once DropdownItem
  static om/IQuery
  (query [this] [:id :label :active? :disabled? :type])
  static om/Ident
  (ident [this props] (dropdown-item-ident props))
  Object
  (render [this]
    (let [{:keys [:label :id :active? :disabled?]} (om/props this)
          active?  (or active? (om/get-computed this :active?))
          onSelect (or (om/get-computed this :onSelect) identity)]
      (if (= :divider label)
        (dom/li #js {:key id :role "separator" :className "divider"})
        (dom/li #js {:key id :className (if disabled? "disabled" "")}
          (dom/a #js {:onClick (fn [evt]
                                 (.stopPropagation evt)
                                 (om/transact! this (into `[(close-all-dropdowns {}) :open?]))
                                 (onSelect id)
                                 false)} label))))))

(let [ui-dropdown-item-factory (om/factory DropdownItem {:keyfn :id})]
  (defn ui-dropdown-item
    "Render a dropdown item. The props are the state props of the dropdown item. The additional by-name
    arguments:

    onSelect - The function to call when a menu item is selected
    "
    [props & {:keys [onSelect]}]
    (ui-dropdown-item-factory (om/computed props {:onSelect onSelect}))))

(defui ^:once Dropdown
  static om/IQuery
  (query [this] [:id :label :open? {:items (om/get-query DropdownItem)} :type])
  static om/Ident
  (ident [this props] (dropdown-ident props))
  Object
  (render [this]
    (let [{:keys [:id :label :items :open?]} (om/props this)
          {:keys [onSelect kind] :or {onSelect identity}} (om/get-computed this)
          active?   (some :active? items)
          open-menu (fn [evt]
                      (.stopPropagation evt)
                      (om/transact! this `[(close-all-dropdowns {}) (set-dropdown-open ~{:id id :open? (not open?)})])
                      false)]
      (dom/div #js {:className (str "button-group " (when open? "open"))}
        (dom/button #js {:className (str "dropdown-toggle"
                                      (when kind (str " btn-" (name kind)))
                                      (when active? " active")) :aria-haspopup true :aria-expanded open? :onClick open-menu}
          label " " (dom/span #js {:className "caret"}))
        (dom/ul #js {:className "dropdown-menu"}
          (map #(ui-dropdown-item % :onSelect onSelect) items))))))

(let [ui-dropdown-factory (om/factory Dropdown {:keyfn :id})]
  (defn ui-dropdown
    [props & {:keys [onSelect kind]}]
    (ui-dropdown-factory (om/computed props {:onSelect onSelect :kind kind}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; NAV (tabs/pills)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def nav-table :bootstrap.nav/by-id)
(def nav-link-table :bootstrap.navitem/by-id)

(defn nav-link-ident [id-or-props]
  (if (map? id-or-props)
    [(:type id-or-props) (:id id-or-props)]
    [nav-link-table id-or-props]))
(defn nav-ident [id-or-props]
  (if (map? id-or-props)
    [nav-table (:id id-or-props)]
    [nav-table id-or-props]))


(defui ^:once NavLink
  static om/IQuery
  (query [this] [:id :label :disabled? :type])
  static om/Ident
  (ident [this props] (nav-link-ident props))
  Object
  (render [this]
    (let [{:keys [:id :label :disabled?]} (om/props this)
          active? (om/get-computed this :active?)]
      (dom/li #js {:role "presentation" :className (cond-> ""
                                                     active? (str " active")
                                                     disabled? (str " disabled"))}
        (dom/a #js {} label)))))

(def ui-nav-link (om/factory NavLink {:keyfn :id}))

(defui ^:once NavItemUnion
  static om/Ident
  (ident [this {:keys [:id type]}] [type id])
  static om/IQuery
  (query [this] {dropdown-table (om/get-query Dropdown) nav-link-table (om/get-query NavLink)})
  Object
  (render [this]
    (let [{:keys [type] :as child} (om/props this)
          {:keys [onSelect] :as computed} (om/get-computed this)]
      (case type
        :bootstrap.navitem/by-id (ui-nav-link (om/computed child computed))
        :bootstrap.dropdown/by-id (ui-dropdown child :onSelect onSelect)
        (dom/p nil "Unknown link type!")))))

(def ui-nav-item (om/factory NavItemUnion {:keyfn :id}))

(defui ^:once Nav
  static om/Ident
  (ident [this props] (nav-ident props))
  static om/IQuery
  (query [this] [:id :kind :layout :active-link-id {:links (om/get-query NavItemUnion)}])
  Object
  (render [this]
    (let [{:keys [:id :kind :layout :active-link-id :links]} (om/props this)]
      (dom/ul #js {:className (str "nav nav-" (name kind) (case layout :justified " nav-justified" :stacked " nav-stacked" ""))}
        (map #(ui-nav-item (om/computed % {:active? (= (:id %) active-link-id)})) links)))))

(def ui-nav (om/factory Nav {:keyfn :id}))

(defn read-local
  [{:keys [query target state ast]} dkey _]
  (when (not target)
    (case dkey
      (let [top-level-prop (nil? query)
            key            (or (:key ast) dkey)
            by-ident?      (util/ident? key)
            union?         (map? query)
            data           (if by-ident? (get-in @state key) (get @state key))]
        {:value
         (cond
           union? (get (om/db->tree [{key query}] @state @state) key)
           top-level-prop data
           :else (om/db->tree query data @state))}))))

(def initial-state {:nav                           [:bootstrap.nav/by-id :main-nav]
                    :bootstrap.dropdown-item/by-id {:report-1 {
                                                               :id    :report-1
                                                               :label "Report 1"
                                                               }
                                                    :report-2 {
                                                               :id    :report-2
                                                               :label "Report 2"
                                                               }}
                    :bootstrap.navitem/by-id       {:home  {
                                                            :id       :home
                                                            :label    "Home"
                                                            :disbled? false
                                                            :type     :bootstrap.navitem/by-id}
                                                    :other {
                                                            :id       :other
                                                            :label    "Other"
                                                            :disbled? false
                                                            :type     :bootstrap.navitem/by-id}}
                    :bootstrap.dropdown/by-id      {:reports {
                                                              :id    :reports
                                                              :label "Reports"
                                                              :items [[:bootstrap.dropdown-item/by-id :report-1] [:bootstrap.dropdown-item/by-id :report-2]]
                                                              :open? true
                                                              :type  :bootstrap.dropdown/by-id}}
                    :bootstrap.nav/by-id           {:main-nav {
                                                               :id             :main-nav
                                                               :kind           :tabs
                                                               :layout         :normal
                                                               :active-link-id :home
                                                               :links          [[:bootstrap.navitem/by-id :home] [:bootstrap.navitem/by-id :other] [:bootstrap.dropdown/by-id :reports]]}}
                    })

(defmulti mutate om/dispatch)
(defmethod mutate :default [{:keys [state]} k {:keys [tab]}] (js/console.log :UNKNOWN))

(defmethod mutate `set-dropdown-open [{:keys [state]} _ {:keys [id open?]}]
  {:action (fn []
             (let [kpath (conj (dropdown-ident id) :open?)]
               (swap! state assoc-in kpath open?)))})

(defmethod mutate `set-dropdown-item-active
  [{:keys [state]} _ {:keys [id active?]}]
  {:action (fn []
             (let [kpath (conj (dropdown-item-ident id) :active?)]
               (swap! state assoc-in kpath active?)))})

(defn- close-all-dropdowns-impl [dropdown-map]
  (reduce (fn [m id] (assoc-in m [id :open?] false)) dropdown-map (keys dropdown-map)))

(defmethod mutate `close-all-dropdowns [{:keys [state]} _ ignored]
  {:action (fn []
             (swap! state update dropdown-table close-all-dropdowns-impl))})

(def parser (om/parser {:read read-local :mutate mutate}))
(defonce reconciler (om/reconciler {:state     (atom initial-state)
                                    :parser    parser
                                    :normalize true
                                    :pathopt   true}))

(defui NavRoot
  static om/IQuery
  (query [this] [{:nav (om/get-query Nav)}])
  Object
  (render [this]
    (let [{:keys [nav]} (om/props this)]
      (dom/div #js {:className "container-fluid"}
        (dom/div #js {:className "row"}
          (dom/div #js {:className "col col-xs-6"} (ui-nav nav)))))))

(om/add-root! reconciler NavRoot (gdom/getElement "app"))


