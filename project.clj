(defproject bonjour_compojure "0.1.0-SNAPSHOT"

  :description "Bonjour compojure"

  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.8"]
                 [liberator "0.12.1"]
                 [ring/ring-json "0.3.1"]
                 [com.novemberain/monger "2.0.0"]
                 [jarohen/nomad "0.7.0"]
                 [lein-light-nrepl "0.0.17"]]

  :plugins [[lein-ring "0.8.11"]]

  :resource-paths ["resources" "config"]

  :ring {:handler bonjour_compojure.handler/app
         :init bonjour_compojure.database/init
         :nrepl {:start? true :port 9001}}

  :immutant {:context-path "/bonjour"
             :nrepl-port 9001}

  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}}

  :repl-options (:nrepl-middleware [lighttable.nrepl.handler/lighttable-ops]))
