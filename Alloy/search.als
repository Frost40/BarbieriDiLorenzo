
sig NameCom {}
sig NameInt {}
sig Domain {}
sig Location {}
sig Requirement {}

sig Company {
    name: one NameCom,        // Name of the company
    avgRating: one Int          // Average feedback rating
}

sig Internship {
    name: one NameInt,             // Internship name
    domain: set Domain,         // Internship domain
    location: one Location,     // Internship location
    requirements: set Requirement, // Skills or qualifications required
    company: one Company        // Linked company
}
sig Visualizer {
    domain: set Domain,
    location: lone Location,
    requirements: set Requirement	
}
sig Result {
    internship: one Internship,
    next: lone Result
}

fact SingleChain {
    all r1, r2: Result | 
        (r1.next = r2.next) => (r1 = r2) // No two results share the same `next`, unless they are the same result
}
fact orderedResults {
    all r: Result | 
        (r.next != none => r.internship.company.avgRating >= r.next.internship.company.avgRating)
}
fact UniqueResultInternship {
    all r1, r2: Result | 
        (r1.internship = r2.internship) => (r1 = r2)
}
fact NoLoops {
    all r: Result | 
       not (r in r.^next) // A result cannot be reachable from itself through its `next` links
}
fact LastResultHasLowestRating {
    all r: Result | 
        r.next = none => 
        all rOther: Result | 
            rOther != r => 
            (rOther.internship.company.avgRating >= r.internship.company.avgRating 
             or some rPrev: Result | rPrev.next = rOther)
}


// Ensure all company names are unique
fact UniqueCompanyNames {
    all c1, c2: Company | c1 != c2 implies c1.name != c2.name
}

pred showNameSearchInternship [target: NameInt] {
      #(Internship.name & target) >= 1 and
	(all i: Internship | i.name = target => some r: Result | r.internship = i) and
	(all r: Result | r.internship.name = target)
}

pred showNameSearchCompany [target: NameCom] {
    #(Internship.company.name & target) >= 1 and
    all i: Internship | i.company.name = target => some r: Result | r.internship = i and
    (all r: Result | r.internship.company.name = target)
}

fact SameCompanyDiffInternshipNames {
	all i1, i2: Internship | i1 != i2 implies (i1.company = i2.company => i1.name != i2.name)  
}
pred searchByName1{
#Company =3
#Internship = 3
some c1, c2 : Company | c1.avgRating != c2.avgRating
some i1, i2 : Internship | i1.company != i2.company
one target : NameInt | showNameSearchInternship[target] and #Result > 2
}
pred searchByName2{
#Company =3
#Internship = 3
some c1, c2 : Company | c1.avgRating != c2.avgRating
some i1, i2 : Internship | i1.company != i2.company
one target : NameCom | showNameSearchCompany[target] and #Result = 1
}
// Predicate to search by domain, requirement, and location
pred searchByAttributes [targetDomain: set Domain, targetRequirements: set Requirement, targetLocation: lone Location] {
    all r: Result | 
        r.internship.domain & targetDomain = targetDomain and // Internship must cover all target domains
        r.internship.requirements & targetRequirements = targetRequirements and // Must cover all target requirements
        (targetLocation = none or r.internship.location = targetLocation) // Location must match if specified
}

pred testSearchByAttributes {
    // Example setup: 3 companies, 3 internships
    #Company = 3
    #Internship = 3
    #Visualizer = 1 
    #Result = 2
    some c1, c2: Company | c1.avgRating != c2.avgRating
    some i1, i2: Internship | i1.company != i2.company
    
    some d1, d2 : Domain, l1, l2 : Location, r1, r2 : Requirement, v: Visualizer | (d1 != d2 and r1 != r2 and l1 != l2 and 
    searchByAttributes[d1 + d2, r1, l1] and v.requirements = r1 and v.location = l1 and v.domain = d1 + d2)
}

//fact quickCheck {
	//all i1, i2: Internship | i1 != i2 => i1.company != i2.company
//}
//fact UniqueInternshipNames {
	//some i1, i2: Internship | i1.name != i2.name
//}
//run searchByName2
run testSearchByAttributes




