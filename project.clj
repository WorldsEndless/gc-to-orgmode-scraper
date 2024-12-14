(defproject scraper "0.1.0-SNAPSHOT"
  :description "Web-scraping and conversion of LDS General Conference to ORG mode using Pandoc"
  :url "http://tech.toryanderson.com"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.12.0"] 
                 [enlive "1.1.6"] ;; the HTML handling library
                 ;;;;;;;; 
                 ;; [cheshire "5.8.0"] for dealing with json, not needed here
          
                 ;; [clj-tagsoup "0.3.0"] based on the Python tagsoup library, but not actually needed here because enlive does great
                 ])
