import javax.inject.Inject

import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter
import filters.{MultiDomainSessionFilter, SessionFilter}

class Filters @Inject()(
 corsFilter: CORSFilter,
 sessionFilter: SessionFilter,
 multiDomainSessionFilter: MultiDomainSessionFilter
) extends DefaultHttpFilters(corsFilter, sessionFilter, multiDomainSessionFilter)
