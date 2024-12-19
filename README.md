# Brinvex Portfolio Activity

_Brinvex Portfolio Activity_ simplifies portfolio tracking by collecting 
transactions and daily asset values from banks and brokers. 
It supports online fetching as well as manual input.
Fetched data is stored locally to prevent repeated requests and improve performance.

### Contact
If you have any questions, feedback, or need assistance, please reach out to _info@brinvex.com_.
I am also open to exploring work partnerships of any kind. Whether you’re interested in collaboration,
integration, or other opportunities, feel free to get in touch - I’d love to hear from you!

### Maven Setup
It's not necessary to include all dependencies.
You only need to import the ones relevant to your specific use case.
For example, if you're using the performance calculation features,
you can include only the ``performance-api`` and ``performance-impl`` dependencies.
If you need integration with IBKR, import the ``connector-ibkr-api`` and ``connector-ibkr-impl`` dependencies.

    <properties>
         <brinvex-ptfactivity.version>1.0.1</brinvex-ptfactivity.version>
    </properties>
    
    <repository>
        <id>repository.brinvex</id>
        <name>Brinvex Repository</name>
        <url>https://github.com/brinvex/brinvex-repo/raw/main/</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>

    <dependency>
        <groupId>com.brinvex</groupId>
        <artifactId>brinvex-ptfactivity-core</artifactId>
        <version>${brinvex-ptfactivity.version}</version>
    </dependency>

    <!-- Optional IBKR connector-->
    <dependency>
        <groupId>com.brinvex</groupId>
        <artifactId>brinvex-ptfactivity-connector-ibkr</artifactId>
        <version>${brinvex-ptfactivity.version}</version>
    </dependency>

    <!-- Optional RVLT connector -->
    <dependency>
        <groupId>com.brinvex</groupId>
        <artifactId>brinvex-ptfactivity-connector-rvlt</artifactId>
        <version>${brinvex-ptfactivity.version}</version>
    </dependency>

    <!-- Optional FIOB connector -->
    <dependency>
        <groupId>com.brinvex</groupId>
        <artifactId>brinvex-ptfactivity-connector-fiob</artifactId>
        <version>${brinvex-ptfactivity.version}</version>
    </dependency>

    <!-- Optional AMND connector -->
    <dependency>
        <groupId>com.brinvex</groupId>
        <artifactId>brinvex-ptfactivity-connector-amnd</artifactId>
        <version>${brinvex-ptfactivity.version}</version>
    </dependency>


### Requirements

Java 23 or above

### License

The _Brinvex Portfolio Activity_ is released under version 2.0 of the Apache License.

## Practical insights and tips

#### IBKR - Symbol Discrepancy in Stock Position

A discrepancy may be observed in the display symbols for stock positions, 
such as those in the German company Siemens (_ISIN: DE0007236101_). 
When purchased on IBIS, the symbol for Siemens stock may appear 
as "SIE" in the TWS platform and the Portfolio screen of the Interactive Brokers web application. 
However, in report statements, including flex statements, the symbol may be listed as "SIEd."
This inconsistency arises due to the use of the primary exchange symbol in statements, 
which may differ from the symbol displayed in other parts of the platform. 
Despite this variation, the stock remains the same, as confirmed by matching ISINs and other details. 
This behavior is a standard design feature of the IBKR platform.

#### IBKR - Delayed Update Effect

Many updates or changes made by the user will only take full effect after one business day. 
For example, when modifying the _Account Alias_, the new value will not immediately appear in the statements. 
Instead, the old value will continue to be displayed until the system processes the update overnight. 
Users should expect this one-business-day period for updates to be fully reflected.

#### IBKR - Account ID change
On August 1, 2024, Interactive Brokers Ireland Limited (IBIE) and 
Interactive Brokers Central Europe Zrt. (IBCE) merged into a single entity. 
All former IBCE clients were moved to IBIE, and they got new Account IDs under IBIE.  
https://www.ibkrguides.com/kb/merger-of-two-eu-broker-dealers.htm
If such a scenario occurs again, be aware that IBKR reports may appear 
inconsistent and unstable for a period. It is advisable to wait a few days 
before using them, allowing time for reports of old accounts to stabilize 
and new account reports to include all important data.
In 2024, a 14-day waiting period was sufficient for the reports to consolidate.

