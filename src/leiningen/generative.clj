(ns leiningen.generative
  (:refer-clojure :exclude [test])
  (:require leiningen.core)
  (:use [leiningen.compile :only [eval-in-project]]
        [leiningen.classpath :only [classpath]]))

(defn- run-generative-tests [project]
  `(do
     (let [path# ~(or (:generative-path project) "test/")]
       (println "Testing generative tests in" path#
                "on" gen/*cores* "cores for"
                gen/*msec* "msec.")
       (let [futures# (gen/test-dirs ~(:generative-path project))]
         (doseq [f# futures#]
           (try
             @f#
             (catch Throwable t#
               (.printStackTrace t#)
               (System/exit -1))
             (finally
              (when-not ~leiningen.core/*interactive?*
                (shutdown-agents)))))))))

(defn- set-generative-path-to-project [project]
  (let [generative-path (str (:root project)
                             java.io.File/separatorChar
                             "generative")]
    (merge {:generative-path generative-path} project)))

(defn- add-generative-path-to-classpath [project]
  (update-in project
             [:extra-classpath-dirs]
             #(conj % (:generative-path project))))

(defn generative
  "Run test.generative tests"
  [project & _]
  (let [new-project (-> project
                        set-generative-path-to-project
                        add-generative-path-to-classpath)]
    (eval-in-project
      new-project
      (run-generative-tests new-project)
      nil
      nil
      '(require '[clojure.test.generative :as gen]))))
