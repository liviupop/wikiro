package org.wikipedia.ro.populationdb.ua.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;

public class Commune {
    private long id;

    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    @Column(name = "id")
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Column(name = "nume")
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Column(name = "populatie")
    public int getPopulation() {
        return population;
    }

    public void setPopulation(final int population) {
        this.population = population;
    }

    @ManyToOne
    @JoinColumn(name = "raion")
    public Raion getRaion() {
        return raion;
    }

    public void setRaion(final Raion raion) {
        this.raion = raion;
    }

    @ElementCollection
    @CollectionTable(name = "comuna_nationalitate", joinColumns = @JoinColumn(name = "comuna"))
    @MapKeyJoinColumn(name = "nationalitate")
    @Column(name = "procent")
    public Map<Language, Double> getLanguageStructure() {
        return languageStructure;
    }

    private String name;
    private String transliteratedName;
    private String romanianName;
    private int population;
    private int town;

    public String getTransliteratedName() {
        return transliteratedName;
    }

    public void setTransliteratedName(final String transliteratedName) {
        this.transliteratedName = transliteratedName;
    }

    public String getRomanianName() {
        return romanianName;
    }

    public void setRomanianName(final String romanianName) {
        this.romanianName = romanianName;
    }

    private Raion raion;
    private Set<Settlement> settlements;
    private final Map<Language, Double> languageStructure = new HashMap<Language, Double>();

    @Column(name = "nivel_oras")
    public int getTown() {
        return town;
    }

    public void setTown(final int town) {
        this.town = town;
    }

    @OneToMany(mappedBy = "commune")
    public Set<Settlement> getSettlements() {
        return settlements;
    }

    public void setSettlements(final Set<Settlement> settlements) {
        this.settlements = settlements;
    }

}
