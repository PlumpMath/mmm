(ns mmm.controller.movie
  (:use [compojure.core :only (defroutes GET POST)]
        [mmm.utils])
  (:require [clojure.string :as str]
            [ring.util.response :as ring]
            [ring.middleware [multipart-params :as mp]]
            [mmm.files :as files]
            [mmm.view.layout :as layout]
            [mmm.view.movies :as view]
            [mmm.model.movie :as model]))


(defn add-poster [params]
  (files/add-poster params))

;; (defn view [id]
;;   (layout/common (view/view (model/getByID id))))

(defn all [movies]
  (layout/common (view/all movies)))

(defn edit [id]
  (layout/common (view/edit (model/getByID id))))

(defn update [id movie-map]
  (model/update id movie-map)
  (ring/redirect "/admin"))

(defroutes routes
  ;(GET ["/movies/:id" :id #"[0-9a-f]+"] [id] (render-request view id))
  (GET "/movies/all" [] (render-request all (model/all)))
  (GET ["/movies/edit/:id" :id #"[0-9a-f]+"] [id] (render-request edit id))
  (POST ["/movies/update/:id" :id #"[0-9a-f]+"] [id & params] (update id params))
  (POST "/movies/add-poster" [& params] (add-poster params)))


