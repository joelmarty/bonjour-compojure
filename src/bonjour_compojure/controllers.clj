(ns bonjour_compojure.controllers
  (:require [liberator.core :refer [defresource log!]]
            [bonjour_compojure.database :as dbapi])
  (:import java.net.URL))

;; before we can plug to a real db...
(defonce bonjours (ref {}))

;; utility crap
(defn check-content-type [ctx content-types]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (or
     (some #{(get-in ctx [:request :headers "content-type"])}
           content-types)
     [false {:message "Unsupported Content-Type"}])
    true))

(defn build-entry-url [request id]
  (URL. (format "%s://%s:%s%s/%s"
                (name (:scheme request))
                (:server-name request)
                (:server-port request)
                (:uri request)
                (str id))))

;; liberator resource-style implementation
(defresource all
  :allowed-methods [:get :post]
  :available-media-types ["application/json"]
  :post! (fn [ctx]
           (dosync (let [bonjour (get-in ctx [:request :params :bonjour])
                         created_bonjour (dbapi/add bonjour)
                         id (:_id created_bonjour)]
                     {::id id})))
  :handle-ok (fn [ctx]
               (let [page (get-in ctx [:request :params :page])
                     count (get-in ctx [:request :params :count])]
                 (dbapi/list page count)))
  :post-redirect? (fn [ctx] {:location (format "/bonjours/%s" (::id ctx))})
  :etag (fn [_] (str (count (dbapi/list))))
  :malformed? #(contains? % :text)
  )

(defresource one [id]
  :allowed-methods [:get :put :delete]
  :available-media-types ["application/json"]
  :exists? (fn [_]
             (let [b (dbapi/find-by-id (read-string id))]
               (if-not (nil? b)
                 {::bonjour b})))
  :existed? false
  :can-put-to-missing? false
  :handle-ok ::bonjour
  :put! (fn [ctx]
          (let [new_bonjour (get-in ctx [:request :params :bonjour])]
            (dbapi/update new_bonjour)))
  :malformed? #(and
                (contains? % :id)
                (contains? % :text))
  :delete! (fn [_]
             (dbapi/delete (:_id ::bonjour)))
  :new? false
  :respond-with-entity? true
)
