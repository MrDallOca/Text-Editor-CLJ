(defproject editor-clj "0.1.0-SNAPSHOT"
  :description "A simple text editor written in Clojure"
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [jline/jline "2.14.2"]] 
  :target-path "target/%s"
  :profiles {:user {:source-paths ["dev"]
                    :dependencies
                    [[org.clojure/tools.namespace "0.2.10"]
                     [org.clojure/test.check "0.9.0"]]

                    :plugins [[cider/cider-nrepl "0.14.0"]]
                    :main user
                    :uberjar-name "user.jar"}})

