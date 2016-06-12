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

(defn index []
  (application
   "Osmium"
   (html
    [:div.container {}
     [:div.title-row {}
      [:div {} [:h2.main-title {} "some title"]]
      [:div {} [:span.right "Current: "]]]
     [:div.session {}
      [:h5.session-title {}  "id"
       [:i {:class "fa fa-circle "}]
       [:i.fa.fa-times.u-pull-right.close-session {}]]
      [:button.playback [:a {:target "_blank"}
                         "Playback"]]
      [:button.playback [:a {:href "https://saucelabs.com/beta/manual"
                             :target "_blank"}
                         "Sauce Labs"]]
      [:p {} "App Version: "]
      [:br]]])))
