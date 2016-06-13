(def project 'osmium)
(def version "0.1.0-SNAPSHOT")

(set-env!
 :source-paths    #{"resources" "src"}
 :resource-paths  #{"resources"}
 :dependencies '[
                 ;; Server
                 [medley "0.8.2"]
                 [com.stuartsierra/component "0.2.3"]
                 [ring "1.3.2"]
                 [fogus/ring-edn "0.3.0"]
                 [compojure "1.4.0"]
                 [clj-http "1.1.0"]
                 [hiccup "1.0.5"]
                 [com.datomic/datomic-free "0.9.5372"]
                 ;; Testing
                 [prone "1.1.1"]
                 [org.clojure/test.check "0.9.0"]
                 [clj-webdriver "0.7.2"
                  :exclusions [commons-logging commons-codec org.apache.httpcomponents/httpclient]]
                 [org.seleniumhq.selenium/selenium-htmlunit-driver "2.52.0"
                  :exclusions [org.eclipse.jetty/jetty-util org.eclipse.jetty/jetty-io com.google.guava/guava commons-logging commons-codec org.apache.httpcomponents/httpclient]]
                 [org.seleniumhq.selenium/selenium-java "2.53.0"
                  :exclusions
                  [org.seleniumhq.selenium/selenium-api org.eclipse.jetty/jetty-io com.google.guava/guava org.seleniumhq.selenium/selenium-support commons-codec org.apache.httpcomponents/httpclient org.seleniumhq.selenium/selenium-remote-driver commons-codec org.apache.httpcomponents/httpclient]]])

(load-data-readers!)

(task-options!
 aot {:namespace   #{'osmium.core}}
 pom {:project     project
      :version     version
      :description "FIXME: write description"
      :url         "http://example/FIXME"
      :scm         {:url "https://github.com/bensu/osmium"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}}
 jar {:main        'osmium.core
      :file        (str "osmium-" version "-standalone.jar")})

(deftask build
  "Build the project locally as a JAR."
  [d dir PATH #{str} "the set of directories to write to (target)."]
  (let [dir (if (seq dir) dir #{"target"})]
    (comp (aot) (pom) (uber) (jar) (target :dir dir))))

(deftask run
  "Run the project."
  [a args ARG [str] "the arguments for the application."]
  (require '[osmium.core :as app])
  (apply (resolve 'app/-main) args))
