package com.liquidsys.coco.client.soap;

import java.util.Iterator;
import java.util.ArrayList;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;
import org.dom4j.QName;

import com.liquidsys.soap.DomUtil;
import com.liquidsys.coco.service.mail.MailService;
import com.liquidsys.coco.service.ServiceException;

public class LmcSearchRequest extends LmcSoapRequest {

	private String mLimit;

	private String mOffset;

	private String mTypes;

	private String mSortBy;

	private String mQuery;

	public void setLimit(String l) {
		mLimit = l;
	}

	public void setOffset(String o) {
		mOffset = o;
	}

	public void setTypes(String t) {
		mTypes = t;
	}

	public void setSortBy(String s) {
		mSortBy = s;
	}

	public void setQuery(String q) {
		mQuery = q;
	}

	public String getLimit() {
		return mLimit;
	}

	public String getOffset() {
		return mOffset;
	}

	public String getTypes() {
		return mTypes;
	}

	public String getSortBy() {
		return mSortBy;
	}

	public String getQuery() {
		return mQuery;
	}

    protected Element createQuery(QName elemName) {
        Element request = DocumentHelper.createElement(elemName);

        // add all the attributes of the SearchRequest element
        addAttrNotNull(request, MailService.A_QUERY_LIMIT, mLimit);
        addAttrNotNull(request, MailService.A_QUERY_OFFSET, mOffset);
        addAttrNotNull(request, MailService.A_SEARCH_TYPES, mTypes);
        addAttrNotNull(request, MailService.A_SORTBY, mSortBy);

        // add the query element
        DomUtil.add(request, MailService.E_QUERY, mQuery);

        return request;
    }
    
	protected Element getRequestXML() {
        return createQuery(MailService.SEARCH_REQUEST);
	}

    
    protected void parseResponse(LmcSearchResponse response,
                                 Element responseXML)
        throws ServiceException, LmcSoapClientException
    {
        // get the offset and more attributes from the <SearchResponse> element
        response.setOffset(DomUtil.getAttr(responseXML, MailService.A_QUERY_OFFSET));
        response.setMore(DomUtil.getAttr(responseXML, MailService.A_QUERY_MORE));

        /*
         * Iterate through the elements and put them in a generic ArrayList.  
         * XXX: Should validate that the correct types are returned.
         */ 
        ArrayList mailItems = new ArrayList();
        for (Iterator it = responseXML.elementIterator(); it.hasNext();) {
            Element e = (Element) it.next();

            // find out what element it is and go process that
            String elementType = e.getQName().getName();
            Object o;
            if (elementType.equals(MailService.E_CONV)) {
                o = parseConversation(e);
            } else if (elementType.equals(MailService.E_MSG)) {
                o = parseMessage(e);
            } else if (elementType.equals(MailService.E_MIMEPART)) {
                o = parseMimePart(e);
            } else if (elementType.equals(MailService.E_CONTACT)) {
                o = parseContact(e);
            } else if (elementType.equals(MailService.E_NOTE)) {
                o = parseNote(e);
            } else {
                // unknown element type as search result
                throw new LmcSoapClientException("Unexpected element type " + elementType);
            }

            // add the object to the ArrayList
            mailItems.add(o);
        }

        response.setResults(mailItems);
    }
    
    protected LmcSoapResponse parseResponseXML(Element responseXML)
	    throws ServiceException, LmcSoapClientException 
    {
		LmcSearchConvResponse response = new LmcSearchConvResponse();
		parseResponse(response, responseXML);
		return response;
	}
}