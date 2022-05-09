(defproject com.github.hden/promecinia "0.1.0-SNAPSHOT"
  :description "Use [promesa promises](https://github.com/funcool/promesa) as lacinia ResolverResults."
  :url "https://github.com/hden/promecinia"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :managed-dependencies [[com.walmartlabs/lacinia "1.0"]
                         [funcool/promesa "8.0.450"]]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.walmartlabs/lacinia]
                 [funcool/promesa]]
  :repl-options {:init-ns promecinia.core}
  :profiles
  {:dev {:dependencies [[superlifter "0.1.3"]
                        [datascript "1.3.13"]]}})
