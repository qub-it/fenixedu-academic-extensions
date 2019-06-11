package org.fenixedu.academic.domain.researchPublication;

import java.util.Comparator;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;

/**
 * 
 * @author shezad
 *
 */
public class ResearchPublication extends ResearchPublication_Base {

    protected ResearchPublication() {
        super();
        setRoot(Bennu.getInstance());
    }

    public static ResearchPublication create(final Person person, final ResearchPublicationType type) {
        final ResearchPublication publication = new ResearchPublication();

        final Optional<ResearchPublication> maxPublication = person.getResearchPublicationsSet().stream()
                .filter(p -> p.getType() == type).max(Comparator.comparing(ResearchPublication::getRelevanceOrder));

        publication.setRelevanceOrder(maxPublication.isPresent() ? maxPublication.get().getRelevanceOrder() + 1 : 1);
        publication.setPerson(person);
        publication.setType(type);

        return publication;
    }

    public void edit(final String title, final String authors, final String publicationData, final Integer year) {
        setTitle(title);
        setAuthors(authors);
        setPublicationData(publicationData);
        setYear(year);
    }

    public void changeRelevanceOrder(boolean increment) {
        final Integer currentOrder = getRelevanceOrder();
        final Integer newOrder = currentOrder + (increment ? -1 : 1);

        final Optional<ResearchPublication> publicationToSwap = findPublicationByRelevanceOrder(newOrder, getPerson(), getType());

        publicationToSwap.ifPresent(preference -> {
            preference.setRelevanceOrder(currentOrder);
            setRelevanceOrder(newOrder);
        });
    }

    private static Optional<ResearchPublication> findPublicationByRelevanceOrder(final Integer relevanceOrder,
            final Person person, final ResearchPublicationType type) {
        return person.getResearchPublicationsSet().stream()
                .filter(p -> p.getRelevanceOrder().equals(relevanceOrder) && p.getType() == type).findFirst();
    }

    /**
     * @return person publications sorted by relevance order
     */
    public static SortedSet<ResearchPublication> findPublicationsSortedByRelevance(final Person person,
            final ResearchPublicationType type) {
        final SortedSet<ResearchPublication> result = new TreeSet<>(Comparator.comparing(ResearchPublication::getRelevanceOrder));
        if (person != null && type != null) {
            result.addAll(person.getResearchPublicationsSet().stream().filter(p -> type.equals(p.getType()))
                    .collect(Collectors.toSet()));
        }
        return result;
    }

    public void delete() {
        final Person person = getPerson();
        final ResearchPublicationType type = getType();
        setPerson(null);
        setType(null);
        setRoot(null);

        // reorder remaining publications
        final AtomicInteger order = new AtomicInteger(0);
        findPublicationsSortedByRelevance(person, type).forEach(rp -> rp.setRelevanceOrder(order.incrementAndGet()));

        super.deleteDomainObject();
    }

}