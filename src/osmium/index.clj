(ns osmium.index
  (:require [hiccup.core :as html :refer [html]]
            [hiccup.page :as page]
            [osmium.book :as book]))

(defn layout [title & content]
  (page/html5
   {:lang "en"}
   [:head
    [:title title]
    (page/include-css "https://cdnjs.cloudflare.com/ajax/libs/skeleton/2.0.4/skeleton.min.css")
    (page/include-css "https://maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css")
    (page/include-css "/css/main.css")
    [:body
     [:div {:class "container"} content ]]]))

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
   [:tr.session-row {:onclick (format "document.location = '/book/%s'" (:db/id book) )}
    [:td.row-left (:book/title book)]
    [:td.row-left (:book/author book)]
    [:td.row-right.center {} (rating-icons (:book/rating book))]]))

(defn table [books]
  (html
   [:div.main {}
    [:table.session-table.u-full-width {}
     [:thead title-row]
     [:tbody {}
      (for [book books]
        (book-row book))]]]))

(defn index [books]
  (layout
   "Osmium"
   (html
    [:div.container {}
     [:div.title-row {}
      [:div {}
       [:h2.main-title {} "Osmium"]
       [:h5 {} [:em {} "Dense books"]]]]
     (table books)])))

;; ======================================================================
;; Book View

(defn book-view [book {:keys [edit?]}]
  (layout
   (str "Osmium - " (:book/title book))
   (html
    [:div.container {}
     [:h2.main-title {} (:book/title book)]
     [:h5 {} [:em {} (:book/author book)]]
     (rating-icons (:book/rating book))
     [:br]
     (if edit?
       [:textarea {}
        (:book/description book)]
       [:p {}
        (:book/description book)])
     [:br]
     (if edit?
       [:button {}
        "Save"]
       [:button {}
        [:a {:href (format "/book/%s?mode=edit" (:db/id book))}
         "Edit"]])])))
