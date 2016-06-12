(ns osmium.index
  (:require [hiccup.core :as html :refer [html]]
            [hiccup.page :as page]))

(defn application [title & content]
  (page/html5
   {:lang "en"}
   [:head
    [:title title]
    (page/include-css "https://cdnjs.cloudflare.com/ajax/libs/skeleton/2.0.4/skeleton.min.css")
    (page/include-css "https://maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css")
    (page/include-css "css/main.css")
    [:body
     [:div {:class "container"} content ]]]))

(def books
  [{:title "I Am a Strange Loop"
    :author "Douglas R. Hofstadter"
    :rating 5}
   {:title "I Am a Strange Loop"
    :author "Douglas R. Hofstadter"
    :rating 5}
   {:title "I Am a Strange Loop"
    :author "Douglas R. Hofstadter"
    :rating 4}
   {:title "I Am a Strange Loop"
    :author "Douglas R. Hofstadter"
    :rating 5}])

(def title-row
  [:tr
   [:th.row-left "Title"]
   [:th "Author"]
   [:th.center.row-right "Rating"]])

(defn rating-icons [r]
  (html
   [:span {}
    (for [i (range r)]
      [:i {:class "fa fa-star"}])]))

(defn book-row [book]
  (html
   [:tr.session-row {}
    [:td.row-left (:title book)]
    [:td.row-left (:author book)]
    [:td.row-right.center {} (rating-icons (:rating book))]]))

(defn table []
  (html
   [:div.main {}
    [:table.session-table.u-full-width {}
     [:thead title-row]
     [:tbody {}
      (for [book books]
        (book-row book))]]]))

(defn index []
  (application
   "Osmium"
   (html
    [:div.container {}
     [:div.title-row {}
      [:div {}
       [:h2.main-title {} "Osmium"]
       [:h5 {} [:em {} "Dense books"]]]]
     (table)])))
