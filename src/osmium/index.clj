(ns osmium.index
  (:require [hiccup.core :as html :refer [html]]
            [hiccup.page :as page]
            [osmium.book :as book]))

(defn logged-in? [session]
  (not (empty? (:user session))))

(defn- footer-link [link title]
  (html
   [:li.navbar-item {}
    [:a.navbar-link {:href link} title]]))

(defn layout [session title & content]
  (page/html5
   {:lang "en"}
   [:head
    [:title title]
    (page/include-css "https://cdnjs.cloudflare.com/ajax/libs/skeleton/2.0.4/skeleton.min.css")
    (page/include-css "https://maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css")
    (page/include-css "/css/main.css")
    [:body
     [:div {:class "container"} content ]
     [:div.footer
      (if (logged-in? session)
        (let [email (get-in session [:user :user/email])]
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

(defn index [session books]
  (layout
   session
   "Osmium"
   (html
    [:div {}
     [:div.title-row {}
      [:div {}
       [:h2.main-title {} "Osmium"]
       [:h5 {} [:em {} "Dense books"]]]]
     (table books)])))

;; ======================================================================
;; Book View

(defn rating-icons-form [book-id]
  (html
   [:form.rating {:action (format "/book/%s/rate" book-id) :method :post}
    (for [i (range 5)]
      [:span {}
       [:input {:id (str "start-" i) :type "radio" :name "rating" :value i}]
       [:label {:for (str "start-" i)}
        [:i {:class "fa fa-star"}]]])
    [:button {:type "submit"} "Rate"]]))

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
        [:button {:type "submit"}
         "Save"]]
       [:div {}
        [:p {} (:book/description book)]
        [:br]
        (when (logged-in? session)
          [:button {}
           [:a {:href (format "/book/%s?mode=edit" (:db/id book))} "Edit"]])])])))

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
      [:input {:name "email" :type "email"}]]
     [:span
      [:label {:for "password"} "Password"]
      [:input {:name "password" :type "password"}]]
     [:br]
     [:button {:type "submit"} "Log in"]])))

(defn sign-up [session]
  (layout
   session
   (str "Osmium - Sign up")
   (html
    [:h1 {} "Sign up"]
    [:form {:action "/signup" :method :post}
     [:span
      [:label {:for "email"} "Email"]
      [:input {:name "email" :type "email"}]]
     [:span
      [:label {:for "password"} "Password"]
      [:input {:name "password" :type "password"}]]
     [:span
      [:label {:for "pass-confirm"} "Confirm Password"]
      [:input {:name "pass-confirm" :type "password"}]]
     [:br]
     [:button {:type "submit"} "Sign up"]])))

(defn- input [name label]
  [:span
   [:label {:for name} label]
   [:input {:name name}]])

(defn user-view [session user]
  (layout
   session
   (str "Osmium - Sign up")
   (html
    [:p {} (:user/email user)]
    [:h4 {} "Change your password"]
    [:form {:action "/update-pass" :method :post}
     [:input {:name "email" :type "hidden" :value (:user/email user)}]
     [:span
      [:label {:for "old-pass"} "Old Password"]
      [:input {:name "old-pass" :type "password"}]]
     [:span
      [:label {:for "new-pass"} "New Password"]
      [:input {:name "new-pass" :type "password"}]]
     [:span
      [:label {:for "pass-confirm"} "Confirm Password"]
      [:input {:name "pass-confirm" :type "password"}]]
     [:br]
     [:button {:type "submit"} "Edit"]])))

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
      [:textarea {:name "description"}]]
     [:button {:type "submit"} "Create"]])))
