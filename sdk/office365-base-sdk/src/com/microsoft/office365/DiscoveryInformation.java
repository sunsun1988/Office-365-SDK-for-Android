package com.microsoft.office365;

import com.microsoft.office365.OfficeEntity;

public class DiscoveryInformation extends OfficeEntity {
    public String getServiceName() {
        return getData("ServiceName").toString();
    }
    
    public String getServiceResourceId() {
        return getData("ServiceResourceId").toString();
    }
    
    public String getCapability() {
        return getData("Capability").toString();
    }
    
    public String getServiceEndpointUri() {
        return getData("ServiceEndpointUri").toString().split("_api")[0];
    }
}
