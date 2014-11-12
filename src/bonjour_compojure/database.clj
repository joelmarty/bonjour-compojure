(ns bonjour_compojure.database
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.conversion :refer [from-db-object to-object-id]]
            [nomad :refer [defconfig]]
            [clojure.java.io :as io]
            [monger.query :as mq]))

(defconfig app-config (io/resource "app.edn"))

(def conn (atom nil))
(def bonjourdb (atom nil))

(defn init
  "Initializes the connection to the database and returns the atom representing the db object.
  Is side-effecting by creating the database infrastructure if it doesn't exist yet.
  Should be called at app startup."
  []
  (let [config (app-config)
        host (:mongo-host config)
        port (:mongo-port config)
        database (:database config)
        collection (:bonjour config)]
    (if (and (nil? host) (nil? port))
      (reset! conn (mg/connect))
      (reset! conn (mg/connect {:host host :port port})))
    (if (not (nil? @conn))
      (do
        (reset! bonjourdb (mg/get-db @conn database))
        (if (not (mc/exists? @bonjourdb collection))
          (mc/create @bonjourdb collection {:capped false}))
        (mc/ensure-index @bonjourdb collection {"date" 1}))
      (throw (RuntimeException.
             (str "The connection to" host ":" port "/" database "failed to open"))))))

(defn list-empty?
  "returns true if the bonjour collection contains any element"
  []
  (let [config (app-config)
        database (:database config)]
    (mc/any? @bonjourdb database)))

(defn find-by-date
  "finds a bonjour by date"
  [date]
  (let [collection (:bonjour (app-config))]
    (from-db-object (mc/find-one @bonjourdb collection {:date date}) true)))

(defn add
  "adds a new bonjour to the db"
  [bonjour]
  (let [collection (:bonjour (app-config))]
    (from-db-object (mc/insert-and-return @bonjourdb collection bonjour) true)))

(defn list
  "lists the bonjour items in the db. Takes optional page and length arguments"
  ([]
    (into [] (mc/find-maps @bonjourdb (:bonjour (app-config)))))
  ([page]
    (list page 10))
  ([page length]
    (mq/with-collection @bonjourdb (:bonjour (app-config))
      (mq/find {})
      (mq/sort {:date -1})
      (mq/paginate :page page :per-page length))))

(defn delete
  "Deletes a bonjour by id. Returns the deleted element"
  [id]
  (let [collection (:bonjour (app-config))
        deleted-bonjour (mc/find-one-as-map @bonjourdb collection {:_id (to-object-id id)})]
    (if (not (nil? deleted-bonjour))
     (do
       (mc/remove-by-id @bonjourdb collection id)
       deleted-bonjour)
     nil)))



