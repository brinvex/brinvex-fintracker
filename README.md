# Brinvex FinTracker

_Brinvex FinTracker_ is a practical tool focused on investment tracking 
offering detailed insights into investment portfolios.
It is capable of interacting with various banks and brokers, 
and it is highly extensible, making it easy to add support for additional institutions.
Designed as both an application for end-users and a library for integration into other systems,
it provides flexible and scalable solutions for financial tracking.

### Contact

If you have any questions, feedback, or need assistance, please reach out to _info@brinvex.com_. 
I am also open to exploring work partnerships of any kind. Whether you’re interested in collaboration, 
integration, or other opportunities, feel free to get in touch—I’d love to hear from you!

### Maven Dependencies

    <properties>
         <brinvex-fintracker.version>0.0.19</brinvex-fintracker.version>
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
        <groupId>com.brinvex.fintracker</groupId>
        <artifactId>brinvex-fintracker-connector-ibkr-api</artifactId>
        <version>${brinvex-fintracker.version}</version>
    </dependency>
    <dependency>
        <groupId>com.brinvex.fintracker</groupId>
        <artifactId>brinvex-fintracker-connector-ibkr-impl</artifactId>
        <version>${brinvex-fintracker.version}</version>
        <scope>runtime</scope>
    </dependency>

## Features

### Account Statement Management

#### Account Statement Online Fetching

_Brinvex FinTracker_ supports online fetching from supported banks and brokers. 
This feature enables the automatic retrieval and updating of account statements, 
minimizing manual input and ensuring data remains current.

#### Account Statement Parsing

_Brinvex FinTracker_ includes statement parsing capabilities, 
allowing to extract all important data from account statements from 
various financial institutions.

#### Account Statement Consolidation

_Brinvex FinTracker_ consolidates financial statements from multiple sources into a unified view, 
facilitating comprehensive analysis and further processing. 

#### Account Statement Storage

_Brinvex FinTracker_ stores financial statements in a simple Document Management System (_DMS_). 
This feature ensures that all statements are securely saved and easily accessible. 
Additionally, the system detects and cleans overlapping statements, 
preventing duplication and maintaining the integrity of data.



## Requirements

- Java 22 or above

## License

- The _Brinvex FinTracker_ is released under version 2.0 of the Apache License.

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
Interactive Brokers Central Europe Zrt. (IBCE) merged into a single entity, 
with all former IBCE clients now serviced by IBIE. 
As a result, former IBCE clients will be assigned new Account IDs under IBIE.  
https://www.ibkrguides.com/kb/merger-of-two-eu-broker-dealers.htm

To manage this transition, we utilize the ````IbkrAccount.migratedAccount```` structure 
to record the old Account ID and the date of migration.

