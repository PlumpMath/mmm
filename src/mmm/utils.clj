(ns mmm.utils
  (:require [net.cgrand.enlive-html :as html]
            [clj-time.core :as time]
            [clj-time.format :as time-fm]
            [clj-time.coerce :as coerce]
            [clj-time.local :as local]  
            [clojure.java.io :as io])
  (:use     [ring.adapter.jetty :only [run-jetty]]
            [ring.util.response :only [response response? charset content-type file-response]]
            [ring.middleware.reload :only [wrap-reload]]
            [ring.middleware.file :only [wrap-file]]
            [ring.middleware.stacktrace :only [wrap-stacktrace]]
            ))

(def ^:dynamic *webdir* (str (.getCanonicalFile (io/file ".")) "/src/tutorial/"))

(defn render [t]
  (apply str t))

(defn render-snippet [s]
  (apply str (html/emit* s)))

(def render-to-response
  (comp #(content-type (charset (response %) "utf-8") "text/html") render))

(defn page-not-found [req]
  {:status 404
   :headers {"Content-type" "text/html"}
   :body "Page Not Found"})

(defn render-request [afn & args]
  (fn [req] (render-to-response (apply afn args)) ))

(def render-to-calendar
  (comp #(content-type (charset (response %) "utf-8") "text/calendar") render))


(defn render-calendar [afn & args]
  (fn [req] (render-to-calendar (apply afn args)) ))

(defn serve-file [filename]
  (file-response
   {:root *webdir*
    :index-files? true
    :html-files? true}))

(defn run-server* [app & {:keys [port] :or {port 8080}}]
  (let [nses (if-let [m (meta app)]
               [(-> (:ns (meta app)) str symbol)]
               [])]
    (println "run-server*" nses)
    (run-jetty
     (-> app
         (wrap-file *webdir*)
         (wrap-reload nses)
         (wrap-stacktrace))
     {:port port :join? false})))

(defmacro run-server [app]
  `(run-server* (var ~app)))

(defmulti parse-int type)
(defmethod parse-int java.lang.Integer [n] n)
(defmethod parse-int java.lang.String [s] (Integer/parseInt s))

(defmacro maybe-substitute
  ([expr] `(if-let [x# ~expr] (html/substitute x#) identity))
  ([expr & exprs] `(maybe-substitute (or ~expr ~@exprs))))

(defmacro maybe-content
  ([expr] `(if-let [x# ~expr] (html/content x#) identity))
  ([expr & exprs] `(maybe-content (or ~expr ~@exprs))))

(defn pluralize [astr n]
  (if (= n 1)
    (str astr)
    (str astr "s")))

(defn alphabetize-by [key items]
  (sort-by #(clojure.string/lower-case (key %)) items))

(defn right-now []
  (time/to-time-zone
   (time/now)
   (time/time-zone-for-offset -6)))


(defn start-of-day [datetime]
  (.withTime datetime 0 0 0 0))

(defn end-of-day [datetime]
  (.withTime datetime 23 59 59 999))

(defn on-sunday [datetime]
  (.withDayOfWeek datetime 7))

(defn yesterday []
  (time/minus (right-now) (time/days 1)))

(defn tomorrow []
  (time/plus (right-now) (time/days 1)))

(defn end-of-this-week
  []
  (end-of-day (on-sunday (right-now))) ;;Returns this Sunday
  )

(defn beginning-of-next-week []
  (start-of-day (time/plus (end-of-this-week) (time/days 1))))

(defn end-of-next-week
  []
  (end-of-day (on-sunday (time/plus (right-now) (time/weeks 1))))) ;;Returns next Sunday

(end-of-this-week)
(beginning-of-next-week)

(defn beginning-of-the-week-after-next []
  (start-of-day (time/plus (end-of-next-week) (time/days 1))))

(defn two-to-four-weeks-out
  []
  (end-of-day (on-sunday (time/plus (right-now) (time/weeks 3))))) ;;Returns three Sundays from now

(def showtime-format (time-fm/formatter-local "yyyy.MM.dd h:mm a"))

(defn read-showtime [showtime-str]
  (time/from-time-zone
   (time-fm/parse showtime-format showtime-str)
   (time/time-zone-for-offset -6)))


(defn write-showtime [showtime]
  (time-fm/unparse
   showtime-format
   (time/to-time-zone
    showtime
    (time/time-zone-for-offset -6))))


(def dd-mm (time-fm/formatter-local "MM/dd"))
(def weekday-month-date (time-fm/formatter-local "EEEE, MMMM d"))
(def am-pm (time-fm/formatter-local "h:mm a"))


(defn earliest-first [dates]
  (sort-by coerce/to-long dates))

(defn earliest-first-comparator [dateA dateB]
  (compare (coerce/to-long dateA) (coerce/to-long dateB)))

(defn earliest-first-string [dateA dateB]
  (earliest-first-comparator
   (time-fm/parse
    weekday-month-date
    dateA)
   (time-fm/parse
    weekday-month-date
    dateB)))


(defn local-time [showtime]
  (time/to-time-zone
   showtime
   (time/time-zone-for-offset -6)))

(defn display-concise-date [date]
  (time-fm/unparse
   dd-mm
   (local-time date)))

(defn display-date [date]
  (time-fm/unparse
   weekday-month-date
   (time/to-time-zone
    date
    (time/time-zone-for-offset -6))))


(defn display-time [date]
  (time-fm/unparse
   am-pm
   (time/to-time-zone
    date
    (time/time-zone-for-offset -6))))

(defn display-date-and-time [showtime]
  (str (display-date showtime)
       " "
       (display-time showtime)))



(defn date-range [showtimes]
  (let [dates (distinct (map display-date showtimes))]
    (if (= (count dates) 1)
      dates
      [(first dates)
       (last dates)])))

(defn display-date-range [showtimes]
  (let [daterange (date-range showtimes)]
    (if (= (count daterange) 1)
      (first daterange)
      [(first daterange)
       (last daterange)])))

(defn listify-items [items]
  (let [length (count items)]
    (cond (= length 1)
          items
          (= length 2)
          [(first items)
           (str " and " (first (rest items)))]
          :else
          (concat
           (map #(str % ", ") (take (- length 1) items))
           [(str " and " (last items))]))))

(defn display-price [price]
  (str "$" price))
