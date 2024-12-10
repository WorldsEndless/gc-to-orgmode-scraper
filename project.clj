(defproject scraper "0.1.0-SNAPSHOT"
  :description "Web-scraping and conversion of LDS General Conference to ORG mode using Pandoc"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.11.0"]
                 [cheshire "5.8.0"]
                 [compojure "1.6.0"]                 
                 [com.mchange/c3p0 "0.9.5.2"]
                 [ring/ring-defaults "0.1.5"]
                 [cheshire "5.8.0"]
                 [clj-http "3.7.0"]
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
                        [ring/ring-mock "0.3.0"]]}})
