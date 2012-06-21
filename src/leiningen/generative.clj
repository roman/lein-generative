(ns leiningen.generative)

(defn- run-generative-tests [paths keep-alive?]
  `(do
     (println "Testing generative tests in" [~@paths]
              "on" gen/*cores* "cores for"
              gen/*msec* "msec.")
     (let [futures# (gen/test-dirs ~@paths)]
       (doseq [f# futures#]
         (try
           @f#
           (catch Throwable t#
             (.printStackTrace t#)
             (System/exit -1))
           (finally
            (when-not ~keep-alive?
              (shutdown-agents))))))))

(defn- add-generative-path-to-classpath [project classpath-id]
  (if-let [path (:generative-path project)]
    (update-in project classpath-id #(conj % path))
    project))

(defn add-generative-dependency [project]
  (if (some #(= 'org.clojure/test.generative (first %)) (:dependencies project))
    project
    (update-in project [:dependencies]
               conj '[org.clojure/test.generative "0.1.4"])))

(defn generative
  "Run test.generative tests"
  [project & _]
  (let [[eip keep-alive? classpath-id paths]
        (or (try (require 'leiningen.core.eval)
                 [(resolve 'leiningen.core.eval/eval-in-project)
                  (not @(resolve 'leiningen.core.main/*exit-process?*))
                  [:test-paths]
                  (if-let [path (:generative-path project)]
                    [path]
                    (:test-paths project))]
                 (catch java.io.FileNotFoundException _))
            (try (require 'leiningen.compile)
                 [(fn [p f g]
                    ((resolve 'leiningen.compile/eval-in-project) p f nil nil g))
                  @(resolve 'leiningen.core/*interactive?*)
                  [:extra-classpath-dirs]
                  [(or (:generative-path project)
                       (:test-path project))]]
                 (catch java.io.FileNotFoundException _)))]
    (let [new-project (-> project
                          add-generative-dependency
                          (add-generative-path-to-classpath classpath-id))]
      (eip new-project
           (run-generative-tests paths keep-alive?)
           '(require '[clojure.test.generative :as gen])))))