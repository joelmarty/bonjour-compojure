(ns bonjour_compojure.test.database
  (:require [clojure.test :refer :all]
            [bonjour_compojure.database :as dbapi]
            [monger.core :as mg]
            [monger.collection :as mc]
            [nomad :refer [defconfig]]
            [clojure.java.io :as io]))

(defconfig app-config (io/resource "app.edn"))

(defn reset-db [connection db-name collection-name]
  (let [db (mg/get-db connection db-name)]
    (if (mc/exists? db collection-name)
      (mc/drop db collection-name))
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
      (reset-db conn test-db test-collection))
    (dbapi/init)
    (test-fn) ;; call to test is explicit
    (let [conn (mg/connect {:host host :port port})]
      (delete-db conn test-db))))

(defn clean-collection [test-fn]
  (let [config (app-config)
        test-db (:database config)
        test-collection (:bonjour config)]
    (test-fn)
    (mc/remove @dbapi/bonjourdb test-collection)))

(defn test-data []
  (map #(array-map :date (format "2014-09-%02d" %) :image (format "2014/09/%02d.jpg" %)) (range 18 28)))

(use-fixtures :once setup)
(use-fixtures :each clean-collection)

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

  (testing "gets a bonjour by id"
    (let [test-collection (:bonjour (app-config))
          documents (into [] (test-data))]
      (mc/insert-batch @dbapi/bonjourdb test-collection documents)
      (let [docs-in-db (dbapi/list)
            one-doc (first docs-in-db)
            found-doc (dbapi/find-by-id (:_id one-doc))]
        (is (= one-doc found-doc)))))

  (testing "adds a bonjour"
    (let [new_bjr {:date "2014-09-17" :image "2014/09/17.jpg"}
          created_bjr (dbapi/add new_bjr)]
      (is (not (nil? created_bjr)))
      (is (contains? created_bjr :_id))
      (is (contains? created_bjr :date))
      (is (contains? created_bjr :image))))

  (testing "gets the list of all bonjour"
    (let [test-collection (:bonjour (app-config))
          documents (into [] (test-data))]
      (mc/insert-batch @dbapi/bonjourdb test-collection documents)
      (let [docs-in-db (dbapi/list)]
        (is (<= 10 (count docs-in-db))))))

  (testing "gets the list of 5 items in first page of bonjours"
    (let [test-collection (:bonjour (app-config))
          documents (into [] (test-data))]
      (mc/insert-batch @dbapi/bonjourdb test-collection documents)
      (let [docs-in-db (dbapi/list 1 5)]
        (is (= 5 (count docs-in-db))))))

  (testing "deletes a bonjour by id"
    (let [test-collection (:bonjour (app-config))
          documents (into [] (test-data))
          last_inserted (last documents)]
      (mc/insert-batch @dbapi/bonjourdb test-collection documents)
      (let [to_delete (dbapi/list 1 1)
            deleted (dbapi/delete (:_id (first to_delete)))]
        (is (= (:date deleted) (:date last_inserted)))
        (is (= (:image deleted) (:image last_inserted))))))
)
