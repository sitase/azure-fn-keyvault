# Sample for using Azure Key vault with Azure Functions

This sample demonstrates how to retrieve secrets programmatically from Azure Key vault in Azure Functions, implemented using azure-functions-java-library. The secret is cached in-memory.

The function is invoked with an HTTP trigger.

## Prerequisites

- Java 8
- Gradle
- Azure CLI

## Preparations

For this sample we have to create a keyvault and give access to it to the local developer and the system managed identity of the deployed function. This way we can develop and debug locally, as well as running in azure. 

### Log in locally

```
az login
```

### Create a Key vault

```
KEYVAULT=simon-testar-keyvault
RESOURCEGROUP=simon-testar

az group create --name "${RESOURCEGROUP}" --location eastus
az keyvault create --name "${KEYVAULT}" --resource-group "${RESOURCEGROUP}" --location westeurope
```


Grant permissions to a specific developer to access the keyvault:
```
 az keyvault set-policy --name "${KEYVAULT}" --upn some.one@hm.com --secret-permissions get
```
Or better, grant permissions to security group:
```
 az keyvault set-policy --name "${KEYVAULT}" --object-id deadbeef-cafe-f00d-a184-540cb8e8b93b  --secret-permissions get
```

### Add a secret
```
az keyvault secret set --vault-name "${KEYVAULT}" --name "asecret" --value "hush-hush"
```

### Configure

## run locally

Start the function host locally:

```
gradle azureFunctionsRun
```

In a different terminal, try it out:
```
curl -w "\n"  http://localhost:7071/api/HttpExample 
```

You should get something like:
```
Hello, hush-hush
```

If you change the secret, the change should be reflected after the cached value expires.

## run in azure

This will not actually work, pending the resolution of this bug: https://github.com/Azure/Azure-Functions/issues/1533

`gradle azureFunctionsDeploy`

Create system assigned identity:

```shell script
 az functionapp identity assign --name simon-testar-keyvault-2 --resource-group "${RESOURCEGROUP}"
```

You get a response like this one:

````json
{
  "principalId": "0d15ea5e-cafe-f00d-beef-defec8eddead",
  "tenantId": "deadfa11-04d4-4e86-875f-e48fa80b9529",
  "type": "SystemAssigned",
  "userAssignedIdentities": null
}
````


On Key vault, configure accesspolicy to read secrets for id of function.

````shell script
az keyvault set-policy --name "${KEYVAULT}" --object-id  "0d15ea5e-cafe-f00d-beef-defec8eddead" --secret-permissions get

````







