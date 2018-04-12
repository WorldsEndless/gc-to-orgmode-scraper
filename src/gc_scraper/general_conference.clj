(ns gc-scraper.general-conference
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as html])
  (:import [java.net URL]
           [java.io File]))

(defn get-title [talk]
  (-> (html/select talk [:div.title-block :div]) first :content first))

(defn get-author [talk]
  (try
    (-> (html/select talk [:a.article-author__name]) first :content first)
    (catch Exception e (do (println "No author found")
                           "<no author>"))))

(defn get-content [talk] (html/select talk [:div.body-block]))

(defn get-references [talk] (html/select talk [:footer.notes :ol]))

(defn make-filename [name]
  (-> name
      (str/replace #" " "-")
      (str/replace #"[^a-zA-Z-]" "")))

(defn pandoc-from-html
  [{:keys [title html-content-string html-references-string author]}]
  (println "Attempting to gen org from " title)
  (-> (clojure.java.shell/sh "pandoc" "-f" "html" "-t" "org"
                             "--wrap" "none"
                             :in (str
                                  "<h1>" title " (" author ")</h1>"
                                  "<h2>Contents</h2>" html-content-string
                                  "<h2>References</h2>" html-references-string))
      :out
      (str/replace #"(?s)#\+BEGIN_HTML.*?#\+END_HTML" "") ; Clear HTML markers for cleaner ORG
      (str/replace #" " " ")
      ;(str/replace #"\n(\w)" "\1") ; remove pandoc's customary line ending
      ))


(defn gc [output-path & [talk-urls]]
  (let [html-talks (map #(html/html-resource (URL. %)) talk-urls)
        single-output-file (str output-path "all.org")]
    (println "writing to " single-output-file)
    (spit single-output-file "#+TITLE: General Conference 2018\n") ;; clear the file first
    (doseq [talk html-talks]
      (let [enlive-html-content (get-content talk)
            org-doc (pandoc-from-html {:title (get-title talk)
                                       :author (get-author talk)
                                       :html-content-string (reduce str (html/emit* enlive-html-content))
                                       :html-references-string (reduce str (html/emit* (get-references talk)))})]
        (do
          (println "Processing " (get-title talk))
          (spit single-output-file org-doc :append true))))))

(defn get-web-gc
  "Get general conferencefrom the website"
  [output-dir-path]
  (let [index-url "https://www.lds.org/general-conference?lang=eng"
        talk-urls (-> index-url URL. html/html-resource (html/select [:a.lumen-tile__link])
                         (->> (map #(str "https://www.lds.org" (get-in % [:attrs :href])))))]
    (gc output-dir-path ; TODO requires a slash at the end, right now
        talk-urls)))
                                        
