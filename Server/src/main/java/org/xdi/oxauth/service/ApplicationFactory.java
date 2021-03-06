/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.persist.PersistenceEntryManagerFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xdi.model.SmtpConfiguration;
import org.xdi.oxauth.crypto.signature.SHA256withECDSASignatureVerification;
import org.xdi.oxauth.model.appliance.GluuAppliance;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.config.ConfigurationFactory.PersistenceConfiguration;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.service.cache.CacheConfiguration;
import org.xdi.service.cache.InMemoryConfiguration;

/**
 * Holds factory methods to create services
 *
 * @author Yuriy Movchan Date: 05/22/2015
 */
@ApplicationScoped
@Named
public class ApplicationFactory {

    @Inject
    private Logger log;

    @Inject
    private ConfigurationFactory configurationFactory;

    @Inject
    private ApplianceService applianceService;

    @Inject
    private Instance<PersistenceEntryManagerFactory> persistenceEntryManagerFactoryInstance;

    @Inject
    private StaticConfiguration staticConfiguration;

    public static final String PERSISTENCE_AUTH_CONFIG_NAME = "persistenceAuthConfig";

    public static final String PERSISTENCE_ENTRY_MANAGER_NAME = "persistenceEntryManager";

    public static final String PERSISTENCE_AUTH_ENTRY_MANAGER_NAME = "persistenceAuthEntryManager";

	@Produces @ApplicationScoped @Named("sha256withECDSASignatureVerification")
    public SHA256withECDSASignatureVerification getBouncyCastleSignatureVerification() {
        return new SHA256withECDSASignatureVerification();
    }

	@Produces @ApplicationScoped
	public CacheConfiguration getCacheConfiguration() {
		CacheConfiguration cacheConfiguration = applianceService.getAppliance().getCacheConfiguration();
		if (cacheConfiguration == null || cacheConfiguration.getCacheProviderType() == null) {
			log.error("Failed to read cache configuration from LDAP. Please check appliance oxCacheConfiguration attribute " +
					"that must contain cache configuration JSON represented by CacheConfiguration.class. Applieance DN: " + applianceService.getAppliance().getDn());
			log.info("Creating fallback IN-MEMORY cache configuration ... ");

			cacheConfiguration = new CacheConfiguration();
			cacheConfiguration.setInMemoryConfiguration(new InMemoryConfiguration());

			log.info("IN-MEMORY cache configuration is created.");
		} else if (cacheConfiguration.getNativePersistenceConfiguration() != null) {
			cacheConfiguration.getNativePersistenceConfiguration().setBaseDn(StringUtils.remove(staticConfiguration.getBaseDn().getUmaBase(), "ou=uma,").trim());
		}
		log.info("Cache configuration: " + cacheConfiguration);
		return cacheConfiguration;
	}

	@Produces @RequestScoped
	public SmtpConfiguration getSmtpConfiguration() {
		GluuAppliance appliance = applianceService.getAppliance();
		SmtpConfiguration smtpConfiguration = appliance.getSmtpConfiguration();
		
		if (smtpConfiguration == null) {
			return new SmtpConfiguration();
		}

		applianceService.decryptSmtpPassword(smtpConfiguration);

    return smtpConfiguration;
	}

    public PersistenceEntryManagerFactory getPersistenceEntryManagerFactory() {
        PersistenceConfiguration persistenceConfiguration = configurationFactory.getPersistenceConfiguration();
        PersistenceEntryManagerFactory persistenceEntryManagerFactory = persistenceEntryManagerFactoryInstance
                .select(persistenceConfiguration.getEntryManagerFactoryType()).get();

        return persistenceEntryManagerFactory;
    }

}