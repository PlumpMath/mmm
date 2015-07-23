(ns mmm.digest
	(:require [mmm.model.screening :as screenings]
			  [mmm.utils :as utils]
		      [clj-time.core :as time]))



(defn get-screenings-by-month [year month]
	(let [month-date (time/date-time year month)
		  first (time/first-day-of-the-month  month-date)
		  last (time/last-day-of-the-month month-date)]
		  (screenings/screenings-in-range first last)))


(defn get-dates [screening]
	(distinct (map time/day (:showtime screening))))


(defn get-venue [screening]
	(:name (:venue screening)))

(defn extract-relevant-data [screening]
	{:title (screenings/screening-title screening)
	 :venue (get-venue screening)
	 :dates (get-dates screening)})


(defn create-listing-from-extract [extract]
	(str
		(:venue extract) 
		": "
		(:title extract)
		" - "
		(apply str (utils/listify-items (:dates extract)))
		"<br/>"))


(defn digest-from-screenings [screenings]
	(let [sorted-screenings (sort-by get-venue screenings)
		  extracts (map extract-relevant-data  sorted-screenings)]
		(apply 
			str 
			(map 
				create-listing-from-extract 
				extracts))))


(defn digest-by-month [year month]
	(digest-from-screenings (get-screenings-by-month year month)))

