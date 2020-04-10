<% (require '[hiccup.core :refer [html]]
    '[respatialized.render :refer :all]
    '[respatialized.structure.fractals :as fractals]
    '[respatialized.holotype :as holotype])
(def metadata {:title "HOLOTYPE///2"})
%>

This is an example of using the un-sandboxed environment of <%=(in-code "comb")%> to our advantage by defining a map of document attributes that will be interpreted at runtime by the rendering function.