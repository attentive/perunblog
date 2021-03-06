(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[perun "0.4.2-SNAPSHOT"]
                 [hiccup "1.0.5"]
                 [pandeiro/boot-http "0.6.3-SNAPSHOT"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-time "0.13.0"]
                 [degree9/boot-npm "1.9.0" :scope "test"] ; npm
                 [deraen/boot-sass "0.3.1" :scope "test"]
                 [adzerk/boot-reload "0.5.2" :scope "test"]
                 [adzerk/boot-cljs "2.1.4" :scope "test"]
                 ])

(require '[clojure.string :as str]
         '[clojure.java.io :as io]
         '[clojure.data.json :as json]
         '[boot.core :as boot]
         '[io.perun :refer [global-metadata markdown draft slug ttr word-count
                            build-date gravatar render collection tags paginate
                            assortment static inject-scripts sitemap rss atom-feed]]
         '[blog.index :as index-view]
         '[blog.post :as post-view]
         '[pandeiro.boot-http :refer [serve]]
         '[degree9.boot-npm :as npm]
         '[deraen.boot-sass :refer [sass]]
         '[adzerk.boot-reload :as reload]
         '[adzerk.boot-cljs :refer [cljs]]
           )

(task-options!
 sass {:source-map true})

(deftask build
  "Build test blog. This task is just for testing different plugins together."
  []
  (comp
   (global-metadata)
   (markdown)
   (draft)
   (slug)
   (ttr)
   (word-count)
   (build-date)
   (gravatar :source-key :author-email :target-key :author-gravatar)
   (render :renderer 'blog.post/render)
   (collection :renderer 'blog.index/render :page "index.html")
   (tags :renderer 'blog.tags/render)
   (paginate :renderer 'blog.paginate/render)
   (assortment :renderer 'blog.assortment/render
               :grouper (fn [entries]
                          (->> entries
                               (mapcat (fn [entry]
                                         (if-let [kws (:keywords entry)]
                                           (map #(-> [% entry]) (str/split kws #"\s*,\s*"))
                                           [])))
                               (reduce (fn [result [kw entry]]
                                         (let [path (str kw ".html")]
                                           (-> result
                                               (update-in [path :entries] conj entry)
                                               (assoc-in [path :entry :keyword] kw))))
                                       {}))))
   (static :renderer 'blog.about/render :page "about.html")
   (inject-scripts :scripts #{"reload_client.js"})
   (sitemap)
   (rss :description "tomlynch.io")
   (atom-feed :filterer :original)
   (target)
   (notify)))

(deftask dev
  []
  (comp (watch)
        (reload/reload)
        (cljs)
        (build)
        (npm/npm :install []
                 :cache-key ::cache)
        (npm/node-modules)
        (sass)
        (serve :resource-root "public")))
