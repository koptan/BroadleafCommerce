/*
 * Copyright 2008-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadleafcommerce.openadmin.web.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.classloader.release.ThreadLocalManager;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.exception.SiteNotFoundException;
import org.broadleafcommerce.common.locale.domain.Locale;
import org.broadleafcommerce.common.site.domain.Site;
import org.broadleafcommerce.common.web.AbstractBroadleafWebRequestProcessor;
import org.broadleafcommerce.common.web.BroadleafCurrencyResolver;
import org.broadleafcommerce.common.web.BroadleafLocaleResolver;
import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.broadleafcommerce.common.web.BroadleafSiteResolver;
import org.broadleafcommerce.common.web.BroadleafTimeZoneResolver;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;
import org.thymeleaf.TemplateEngine;

import java.lang.reflect.Field;
import java.util.TimeZone;

import javax.annotation.Resource;


/**
 * 
 * @author Phillip Verheyden
 * @see {@link org.broadleafcommerce.common.web.BroadleafRequestFilter}
 */
@Component("blAdminRequestProcessor")
public class BroadleafAdminRequestProcessor extends AbstractBroadleafWebRequestProcessor {

    protected final Log LOG = LogFactory.getLog(getClass());

    @Resource(name = "blSiteResolver")
    protected BroadleafSiteResolver siteResolver;

    @Resource(name = "messageSource")
    protected MessageSource messageSource;
    
    @Resource(name = "blLocaleResolver")
    protected BroadleafLocaleResolver localeResolver;
    
    @Resource(name = "blAdminTimeZoneResolver")
    protected BroadleafTimeZoneResolver broadleafTimeZoneResolver;
    
    @Resource(name = "blCurrencyResolver")
    protected BroadleafCurrencyResolver currencyResolver;

    @Override
    public void process(WebRequest request) throws SiteNotFoundException {
        Site site = siteResolver.resolveSite(request);

        BroadleafRequestContext brc = new BroadleafRequestContext();
        BroadleafRequestContext.setBroadleafRequestContext(brc);
        
        brc.setSite(site);
        brc.setWebRequest(request);
        brc.setIgnoreSite(site == null);
        
        Locale locale = localeResolver.resolveLocale(request);
        brc.setLocale(locale);
        
        brc.setMessageSource(messageSource);
        
        TimeZone timeZone = broadleafTimeZoneResolver.resolveTimeZone(request);
        brc.setTimeZone(timeZone);
        
        BroadleafCurrency currency = currencyResolver.resolveCurrency(request);
        brc.setBroadleafCurrency(currency);
    }

    @Override
    public void postProcess(WebRequest request) {
        ThreadLocalManager.remove();
        //temporary workaround for Thymeleaf issue #18 (resolved in version 2.1)
        //https://github.com/thymeleaf/thymeleaf-spring3/issues/18
        try {
            Field currentProcessLocale = TemplateEngine.class.getDeclaredField("currentProcessLocale");
            currentProcessLocale.setAccessible(true);
            ((ThreadLocal) currentProcessLocale.get(null)).remove();

            Field currentProcessTemplateEngine = TemplateEngine.class.getDeclaredField("currentProcessTemplateEngine");
            currentProcessTemplateEngine.setAccessible(true);
            ((ThreadLocal) currentProcessTemplateEngine.get(null)).remove();

            Field currentProcessTemplateName = TemplateEngine.class.getDeclaredField("currentProcessTemplateName");
            currentProcessTemplateName.setAccessible(true);
            ((ThreadLocal) currentProcessTemplateName.get(null)).remove();
        } catch (Throwable e) {
            LOG.warn("Unable to remove Thymeleaf threadlocal variables from request thread", e);
        }
    }

}
