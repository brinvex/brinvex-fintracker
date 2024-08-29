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
         <brinvex-fintracker.version>0.0.4</brinvex-fintracker.version>
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

