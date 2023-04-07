(ns gc-scraper.general-conference
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as html])
  (:import [java.net URL]
           [java.io File]))

(def source-urls
  {:domain "https://www.churchofjesuschrist.org"
   :general-conference {:substring "/study/general-conference/2023/04"
                        :suffix "?lang=eng"}
   :come-follow-me-2022 {:substring "/study/manual/come-follow-me-for-individuals-and-families-old-testament-2022"
                         :suffix "?lang=eng"}
   :come-follow-me-2023 {:substring "/study/manual/come-follow-me-for-individuals-and-families-new-testament-2023"
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
  (let [title-string (cond-> title
                       author (str " ( " author ")"))]

    (println "Attempting to gen org from " title)
    (-> (clojure.java.shell/sh "pandoc" "-f" "html" "-t" "org"
                               "--wrap" "none"
                               :in (str
                                    "<h1>" title-string "</h1>"
                                    "<h2>Contents</h2>" html-content-string))
        :out
        (str/replace #"(?s)#\+BEGIN_HTML.*?#\+END_HTML" "") ; Clear HTML markers for cleaner ORG
        (str/replace #" " " "))
      ))

(defn pandoc?
  "Is pandoc installed on the system?"
  []
  (try (clojure.java.shell/sh "pandoc" "-v")
       (catch java.io.IOException _ false)))

(defn collect-content
  "Given an output dir and all the talk URLs, produce each file of pandoc results of the content of each talk"
 [output-path & [{:keys [urls file-topline all-file-name]}]]
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

(defn collect-cfm-content
  "Given an output dir and all the talk URLs, produce each file of pandoc results of the content of each talk"
 [output-dir-path & {:keys [urls file-topline all-file-name]}]
  (if-not (pandoc?)
    (throw (ex-info "Pandoc not found on system" {:cause :no-pandoc}))
    (let [file-topline (or file-topline "#+TITLE: Come Follow Me\n")
          all-file-name (or all-file-name "gc-all.org")
          html-content (map #(html/html-resource (URL. %)) urls)
          single-output-file (str output-dir-path all-file-name)]
      (println "writing to " single-output-file)
      (spit single-output-file file-topline) ;; clear the file first
      (doseq [chapter html-content] ; (def chapter (second html-content))
        (let [enlive-html-content (get-content chapter)
              org-doc (pandoc-from-html {:title (get-title chapter)
                                        :html-content-string (reduce str (html/emit* enlive-html-content))
                                         ;:html-references-string (reduce str (html/emit* (get-references chapter)))
                                         })]
          (do
            (println "Processing " (get-title chapter))
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
                     {:urls talk-urls}))
  )
;; (def urls talk-urls)

(defn get-come-follow-me
  "Get Come Follow Me from the website"
  [output-dir-path]
  (let [domain (source-urls :domain)
        cfm-substring (get-in source-urls [:come-follow-me-2023 :substring])
        lang (get-in source-urls [:come-follow-me-2023 :suffix])
        index-url (str domain cfm-substring lang)
        chapter-urls (-> index-url URL. html/html-resource
                      (html/select [:li :a])
                      (->> (map #(str domain (get-in % [:attrs :href])))
                           (filter #(re-find
                                     (re-pattern
                                      (str cfm-substring "/"))
                                     %))))
        file-topline "#+TITLE: Come Follow Me 2023: New Testament"
        all-file-name "cfm2023.org"
        urls chapter-urls
        cfm-data {:file-topline file-topline
                  :all-file-name all-file-name
                  :urls urls}]
    (collect-cfm-content output-dir-path ; TODO requires a slash at the end, right now
        chapter-urls)))

(comment
  (let [gc-path "/home/torysa/Documents/Gospel_Files/General_Conference/2023-1/"
        cfm-output-dir-path "/home/torysa/Documents/Gospel_Files/Come-Follow-Me/2023"
        output-dir-path "/home/torysa/Documents/Gospel_Files/General_Conference/2023-1/"]
    #_(get-come-follow-me output-dir-path)
    (get-web-gc gc-path)
    ))
