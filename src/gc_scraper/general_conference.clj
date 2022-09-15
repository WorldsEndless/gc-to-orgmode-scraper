(ns gc-scraper.general-conference
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as html])
  (:import [java.net URL]
           [java.io File]))

(def source-urls
  {:domain "https://www.churchofjesuschrist.org"
   :general-conference {:substring "/study/general-conference/2022/04"
                        :suffix "?lang=eng"}
   :come-follow-me-2022 {:substring "/study/manual/come-follow-me-for-individuals-and-families-old-testament-2022"
                         :suffix "?lang=eng"}
   })
(defn get-title [talk]
  (-> (html/select talk [:head :title]) first :content first))

(defn get-author [talk]
  (try
    (-> (html/select talk [:p.author-name]) first :content first)
    (catch Exception e (do (println "No author found")
                           "<no author>"))))

(defn get-content [talk] (html/select talk [:article]))

;; DOESN'T WORK with new footnote format
;; (defn get-references [talk] (html/select talk [:footer.notes :ol]))
;;; 

(defn make-filename [name]
  (-> name
      (str/replace #" " "-")
      (str/replace #"[^a-zA-Z-]" "")))

(defn pandoc-from-html
  [{:keys [title html-content-string author]}]
  (println "Attempting to gen org from " title)
  (-> (clojure.java.shell/sh "pandoc" "-f" "html" "-t" "org"
                             "--wrap" "none"
                             :in (str
                                  "<h1>" title " (" author ")</h1>"
                                  "<h2>Contents</h2>" html-content-string))
      :out
      (str/replace #"(?s)#\+BEGIN_HTML.*?#\+END_HTML" "") ; Clear HTML markers for cleaner ORG
      (str/replace #" " " ")))

(defn pandoc?
  "Is pandoc installed on the system?"
  []
  (try (clojure.java.shell/sh "pandoc" "-v")
       (catch java.io.IOException _ false)))

(defn collect-content
  "Given an output dir and all the talk URLs, produce each file of pandoc results of the content of each talk"
 [output-path & {:keys [urls file-topline all-file-name]}]
  (if-not (pandoc?)
    (throw (ex-info "Pandoc not found on system" {:cause :no-pandoc}))
    (let [file-topline (or file-topline "#+TITLE: General Conference\n")
          all-file-name (or all-file-name "gc-all.org")
          html-content (map #(html/html-resource (URL. %)) urls)
          single-output-file (str output-path all-file-name)]
      (println "writing to " single-output-file)
      (spit single-output-file file-topline) ;; clear the file first
      (doseq [talk html-content]
        (let [enlive-html-content (get-content talk)
              org-doc (pandoc-from-html {:title (get-title talk)
                                         :author (get-author talk)
                                         :html-content-string (reduce str (html/emit* enlive-html-content))
                                         ;:html-references-string (reduce str (html/emit* (get-references talk)))
                                         })]
          (do
            (println "Processing " (get-title talk))
            (spit single-output-file org-doc :append true)))))))

(defn get-web-gc
  "Get general conference from the website"
  [output-dir-path]
  (let [domain (source-urls :domain)
        conference-substring (get-in source-urls [:general-conference :substring])
        lang (get-in source-urls [:general-conference :suffix])
        index-url (str domain conference-substring lang)
        talk-urls (-> index-url URL. html/html-resource
                      (html/select [:li :a])
                      (->> (map #(str domain (get-in % [:attrs :href])))
                           (filter #(re-find
                                     (re-pattern
                                      (str conference-substring "/"))
                                     %))))]
    (collect-content output-dir-path ; TODO requires a slash at the end, right now
       {:urls talk-urls})))

(defn get-come-follow-me
  "Get Come Follow Me from the website"
  [output-dir-path]
  (let [domain (source-urls :domain)
        conference-substring (get-in source-urls [:general-conference :substring])
        lang (get-in source-urls [:general-conference :suffix])
        index-url (str domain conference-substring lang)
        talk-urls (-> index-url URL. html/html-resource
                      (html/select [:li :a])
                      (->> (map #(str domain (get-in % [:attrs :href])))
                           (filter #(re-find
                                     (re-pattern
                                      (str conference-substring "/"))
                                     %))))]
    (collect-content output-dir-path ; TODO requires a slash at the end, right now
        talk-urls)))

                                        
(comment
    (let [output-dir-path "/home/torysa/Documents/Gospel_Files/Come-Follow-Me/"]
      (get-web-gc output-dir-path)))
