(ns curl-builder.core
  (:require [clojure.string :as clj.str]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:import [goog.ui IdGenerator]))

(enable-console-print!)

(def app-state (atom {:uri ""
                      :request-methods ["GET" "POST" "PUT" "DELETE" "PATCH"]
                      :request-method "GET"
                      :use-request-parameters true
                      :request-parameters []
                      :raw-body ""
                      :headers []
                      :use-basic-auth false
                      :basic-auth-username ""
                      :basic-auth-password ""}))

(defn guid []
  (.getNextUniqueId (.getInstance IdGenerator)))

(defn update [cursor key event]
  (om/update! cursor assoc key (.. event -target -value)))

(defn checked [cursor key event]
  (om/update! cursor assoc key (.. event -target -checked)))

(defn add-item [items owner]
  (let [name-input (om/get-node owner "name")
        value-input (om/get-node owner "value")
        id (guid)]
    (om/update! items conj {:id id
                            :name (.. name-input -value trim)
                            :value (.. value-input -value trim)})
    (set! (.-value name-input) "")
    (set! (.-value value-input) "")))

(defn add-row-component [items owner]
  (om/component
    (dom/tr nil
      (dom/td nil
        (dom/input #js {:ref "name", :type "text"}))
      (dom/td nil
        (dom/input #js {:ref "value", :type "text"}))
      (dom/td nil
        (dom/button #js {:className "pure-button button-xsmall"
                         :onClick #(add-item items owner)}
                    "+ add")))))

(defn table-row-component [item owner {:keys [items]}]
  (om/component
    (let [id (:id item)
          remove (fn [_]
                   (om/update! items
                     (fn [items] (into [] (remove #(= (:id %) id) items)))))]
     (dom/tr nil
       (dom/td nil
         (dom/input #js {:type "text"
                         :value (:name item)
                         :onChange #(update item :name %)}))
       (dom/td nil
         (dom/input #js {:type "text"
                         :value (:value item)
                         :onChange #(update item :value %)}))
       (dom/td nil
         (dom/button #js {:className "pure-button button-xsmall"
                          :onClick remove}
                     "- remove"))))))

(defn table-component [items owner]
  (om/component
    (dom/table #js {:className "pure-table", :style #js {:display "inline-block"}}
      (dom/thead nil
        (dom/tr nil
          (dom/th nil "Name")
          (dom/th nil "Value")
          (dom/th nil)))

      (dom/tbody nil
        (om/build add-row-component items)
        (om/build-all table-row-component items
          {:key :id
           :opts {:items items}})))))

(om/root
  app-state
  (fn [app owner]
    (dom/div #js {:className "pure-form"}
      (dom/h1 nil "Curl Request Builder")
      (dom/fieldset #js {:className "pure-form-aligned"}

        (dom/div #js {:className "pure-control-group"}
          (dom/label #js {:htmlFor "uri-input"} "URI")
          (dom/input #js {:id "uri-input"
                          :type "text"
                          :placeholder ":http://..."
                          :onChange #(update app :uri %)}))

        (dom/div #js {:className "pure-control-group"}
          (dom/label #js {:htmlFor "request-types"} "Request Type")
          (dom/select #js {:id "request-types"
                           :value (:request-method app)
                           :onChange #(update app :request-method %)}
            (into [] (map (fn [method]
                            (dom/option #js {:value method} method))
                          (:request-methods app)))))

        (dom/div #js {:className "pure-control-group"}
          (dom/label nil
            (dom/input #js {:type "radio"
                            :checked (:use-request-parameters app)
                            :onClick #(om/transact! app :use-request-parameters (constantly true))})
            " Request Parameters")
          (om/build table-component (:request-parameters app)))

        (dom/div #js {:className "pure-control-group"}
          (dom/label nil
            (dom/input #js {:type "radio"
                            :checked (not (:use-request-parameters app))
                            :onClick #(om/transact! app :use-request-parameters (constantly false))})
            " Raw Body")
          (dom/textarea #js {:className "mono pure-input-2-3"
                             :placeholder "a=1&b=2...."
                             :value (:raw-body app)
                             :onChange #(update app :raw-body %)}))

        (dom/div #js {:className "pure-control-group"}
          (dom/label nil "Headers")
          (om/build table-component (:headers app)))

        (dom/div #js {:className "pure-controls"}
          (dom/label #js {:htmlFor "basic-auth", :className "pure-checkbox"}
            (dom/input #js {:id "basic-auth"
                            :type "checkbox"
                            :checked (:use-basic-auth app)
                            :onChange #(checked app :use-basic-auth %)})
            " Use Basic Authentication"))

        (dom/div #js {:className "pure-control-group"}
          (dom/label #js {:htmlFor "basic-auth-username"} "Username")
          (dom/input #js {:id "basic-auth-username"
                          :type "text"
                          :value (:basic-auth-username app)
                          :onChange #(update app :basic-auth-username %)}))

        (dom/div #js {:className "pure-control-group"}
          (dom/label #js {:htmlFor "basic-auth-password"} "Password")
          (dom/input #js {:id "basic-auth-password"
                          :type "text"
                          :value (:basic-auth-password app)
                          :onChange #(update app :basic-auth-password %)}))

        (dom/div #js {:className "pure-control-group"}
          (dom/label nil "Curl Request")
          (dom/div #js {:className "mono", :style #js {:display "inline-block"}} (pr-str app))))))

  (. js/document (getElementById "app")))
