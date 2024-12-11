(defproject scraper "0.1.0-SNAPSHOT"
  :description "Web-scraping and conversion of LDS General Conference to ORG mode using Pandoc"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [cheshire "5.13.0"]
                 [compojure "1.7.1"]                 
                 [com.mchange/c3p0 "0.10.1"]
                 [ring/ring-defaults "0.5.0"]
                 [cheshire "5.13.0"]
                 [clj-http "3.13.0"]
                 [enlive "1.1.6"]
                 [clj-tagsoup "0.3.0" :exclusions [org.clojure/clojure]]
                                        ;[clj-tagsoup "0.3.0"]
                 ]
  :plugins [[lein-ring "0.9.7"]]
  :migratus {:store :database
             :migrations-dir "sql"}
  :immutant {
             :war {
                   :name "scraper-%v%t"
                   :resource-paths ["war-resources"]
                   :context-path "/"}}
  :ring {:handler scraper.handler/app
         :uberwar-name "scraper.war"}
  
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.4.0"]]}})
