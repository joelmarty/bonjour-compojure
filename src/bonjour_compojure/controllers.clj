(ns bonjour_compojure.controllers
  (:require [liberator.core :refer [defresource log!]])
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
  :handle-ok (fn [_] (vals @bonjours))
  :post! (fn [ctx]
           (dosync (let [bonjour (get-in ctx [:request :params :bonjour])
                         id (+ 1 (count @bonjours))
                         new_bonjour (assoc bonjour :id id)]
                     (alter bonjours assoc id new_bonjour)
                     {::id id})))
  :post-redirect? (fn [ctx] {:location (format "/bonjours/%s" (::id ctx))})
  :etag (fn [_] (str (count @bonjours)))
  :malformed? #(contains? % :text)
  )

(defresource one [id]
  :allowed-methods [:get :put :delete]
  :available-media-types ["application/json"]
  :exists? (fn [_]
             (let [b (get @bonjours (read-string id))]
               (if-not (nil? b)
                 {::bonjour b})))
  :existed? (fn [_] (nil? (get @bonjours (read-string id) ::sentinel)))
  :can-put-to-missing? false
  :handle-ok ::bonjour
  :put! (fn [ctx]
          (dosync
           (let [new_bonjour (get-in ctx [:request :params :bonjour])]
             (alter bonjours assoc (read-string id) new_bonjour))))
  :malformed? #(and
                (contains? % :id)
                (contains? % :text))
  :delete! (fn [_]
             (dosync
              (alter bonjours assoc (read-string id) nil)))
  :new? (fn [_] (nil? (get @bonjours id ::sentinel)))
)
