(ns bonjour_compojure.test.database
  (:require [clojure.test :refer :all]
            [bonjour_compojure.database :as dbapi]
            [monger.core :as mg]
            [monger.collection :as mc]
            [nomad :refer [defconfig]]
            [clojure.java.io :as io]))

(defconfig app-config (io/resource "app.edn"))

(defn create-db [connection db-name collection-name]
  (let [db (mg/get-db connection db-name)]
    (if (mc/exists? db collection-name)
      (mc/drop db collection-name))
    (mc/create db collection-name {:capped false})
    (mg/disconnect connection)))

(defn delete-db [connection db-name]
  (mg/drop-db connection db-name)
  (mg/disconnect connection))

(defn setup [test-fn]
  (let [config (app-config)
        host (:mongo-host config)
        port (:mongo-port config)
        test-db (:database config)
        test-collection (:bonjour config)]
    (let [conn (mg/connect {:host host :port port})]
      (create-db conn test-db test-collection))
    (dbapi/init)
    (test-fn) ;; call to test is explicit
    (let [conn (mg/connect {:host host :port port})]
      (delete-db conn test-db))))

(use-fixtures :once setup)

(deftest test-db

  (testing "the connection was created succesfully"
    (is (instance? com.mongodb.DBApiLayer @dbapi/bonjourdb)))

  (testing "gets a bonjour by date"
    (let [date "2014-09-16"
          test-collection (:bonjour (app-config))]
      (mc/insert @dbapi/bonjourdb test-collection {:date date})
      (let [created-bonjour (dbapi/find-by-date date)]
        (is (not (nil? created-bonjour)))
        (is (= (:date created-bonjour) "2014-09-16")))))
  )
