(def project 'osmium)
(def version "0.1.0-SNAPSHOT")

(set-env!
 :source-paths    #{"resources" "src" "test"}
 :resource-paths  #{"resources"}
 :target-path     "target"
 :dependencies '[[com.pojosontheweb/monte-repack "1.0"]
                 [clj-webdriver "0.7.2"]
                 [org.seleniumhq.selenium/selenium-htmlunit-driver "2.52.0"
                  :exclusions [org.eclipse.jetty/jetty-util org.eclipse.jetty/jetty-io]]
                 [org.seleniumhq.selenium/selenium-java "2.53.0"
                  :exclusions
                  [org.seleniumhq.selenium/selenium-api org.eclipse.jetty/jetty-io org.seleniumhq.selenium/selenium-support org.seleniumhq.selenium/selenium-remote-driver]]])

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
      :file        (str "osmium-" version "-standalone.jar")}
 push {:repo "clojars"})

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
