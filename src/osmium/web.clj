(ns osmium.web
  (:use [medley.core])
  (:require [hiccup.core :as html :refer [html]]
            [hiccup.page :as page]
            [osmium.book :as book]
            [osmium.user :as user]))

(defn- uuid-id []
  (str "osmium-" (random-uuid)))

(defn kw->str [k]
  (str (.-sym k)))

(defmacro action-map [action-type m]
  (let [action-id (uuid-id)]
    `(cond-> (-> ~m
                 (update :class (partial str "osmium-action "))
                 (assoc :data-action ~(kw->str action-type)))
       (nil? (:id ~m)) (assoc :id ~action-id))))

(defn logged-in? [session]
  (not (empty? (:user session))))

(defn- footer-link [link title]
  (html
   [:li.navbar-item {}
    [:a (action-map :click {:class "navbar-link"
                            :href link})
     title]]))

(defn layout [session title & content]
  (page/html5
   {:lang "en"}
   [:head
    [:title title]
    (page/include-css "https://cdnjs.cloudflare.com/ajax/libs/skeleton/2.0.4/skeleton.min.css")
    (page/include-css "https://maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css")
    (page/include-css "/css/main.css")
    [:body
     [:div {:class "container"}
      [:h1 {} [:a {:href "/"} "Osmium"]]
      content]
     [:div.footer
      (if (logged-in? session)
        (let [email (get-in session [:user ::user/email])]
          [:div.container {}
           [:ul.navbar-list {}
            (footer-link (format "/user/%s" email) email)
            (footer-link "/logout" "Log out")
            (footer-link "/book" "Add Book")]])
        [:div.container {}
         [:ul.navbar-list {}
          (footer-link "/login" "Login")
          (footer-link "/signup" "Sign up")]])]]]))

(def title-row
  [:tr
   [:th.row-left "Title"]
   [:th "Author"]
   [:th.center.row-right "Rating"]])

(defn rating-icons [r]
  (html
   [:span.rating {}
    (for [i (range (or r 0))]
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

(defn index [session books]
  (layout
   session
   "Osmium"
   (html
    [:div {}
     (table books)])))

;; ======================================================================
;; Book View

(defn rating-icons-form [book-id]
  (html
   [:form.rating {:action (format "/book/%s/rate" book-id) :method :post}
    (for [i (range 5)]
      [:span {}
       [:input (action-map :click {:id (str "start-" i) :type "radio" :name "rating" :value i})
        [:label {:for (str "start-" i)}
         [:i {:class "fa fa-star"}]]]])
    [:button (action-map :click {:class "rate" :type "submit"}) "Rate"]]))

(defn book-view [session book {:keys [edit?]}]
  (layout
   session
   (str "Osmium - " (:book/title book))
   (html
    [:div.book-view {}
     [:h2.main-title {} (:book/title book)]
     [:h5 {} [:em {} (:book/author book)]]
     (if (logged-in? session)
       (rating-icons-form (:db/id book))
       (rating-icons (:book/rating book)))
     (if edit?
       [:form {:action (format "/book/%s/description" (:db/id book))
               :method :post}
        [:textarea {:name "book/description"}
         (:book/description book)]
        [:br]
        [:button (action-map :click {:type "submit"})
         "Save"]]
       [:div {}
        [:p {} (:book/description book)]
        [:br]
        (when (logged-in? session)
          [:button {}
           [:a (action-map :click {:href (format "/book/%s?mode=edit" (:db/id book))})
            "Edit"]])])])))

;; ======================================================================
;; User View

(defn log-in [session]
  (layout
   session
   (str "Osmium - Log in")
   (html
    [:h1 {} "Log in"]
    [:form {:action "/login" :method :post}
     [:span
      [:label {:for "email"} "Email"]
      [:input (action-map ::user/email {:name "email" :type "email"})]]
     [:span
      [:label {:for "password"} "Password"]
      [:input (action-map ::user/password {:name "password" :type "password"})]]
     [:br]
     [:button (action-map :click {:type "submit"}) "Log in"]])))

(defn sign-up [session]
  (layout
   session
   (str "Osmium - Sign up")
   (html
    [:h2 {} "Sign up"]
    [:form {:action "/signup" :method :post}
     [:span
      [:label {:for "email"} "Email"]
      [:input (action-map ::user/email {:id "email" :name "email" :type "email"})]]
     [:span
      [:label {:for "password"} "Password"]
      [:input (action-map ::user/password {:name "password" :type "password"})]]
     [:span
      [:label {:for "pass-confirm"} "Confirm Password"]
      [:input (action-map ::user/password {:name "pass-confirm" :type "password"})]]
     [:br]
     [:button (action-map :click {:type "submit"}) "Sign up"]])))

(defn- input [name label]
  [:span
   [:label {:for name} label]
   [:input (action-map :fill {:name name})]])

(defn user-view [session user]
  (layout
   session
   (str "Osmium - Sign up")
   (html
    [:p {} (::user/email user)]
    [:h4 {} "Change your password"]
    [:form {:action "/update-pass" :method :post}
     [:input (action-map ::user/email {:name "email" :type "hidden" :value (::user/email user)})]
     [:span
      [:label {:for "old-pass"} "Old Password"]
      [:input (action-map ::user/password {:name "old-pass" :type "password"})]]
     [:span
      [:label {:for "new-pass"} "New Password"]
      [:input (action-map ::user/password {:name "new-pass" :type "password"})]]
     [:span
      [:label {:for "pass-confirm"} "Confirm Password"]
      [:input (action-map ::user/password {:name "pass-confirm" :type "password"})]]
     [:br]
     [:button (action-map :click {:type "submit"}) "Edit"]])))

(defn new-book [session]
  (layout
   session
   (str "Osmium - New Book")
   (html
    [:h2 "New Book"]
    [:form {:action "/book" :method :post}
     (input "title" "Title")
     (input "author" "Author")
     (input "iban" "IBAN")
     [:span
      [:label {:for "description"} "Description"]
      [:textarea (action-map :fill {:name "description"})]]
     [:button (action-map :click {:type "submit"}) "Create"]])))
