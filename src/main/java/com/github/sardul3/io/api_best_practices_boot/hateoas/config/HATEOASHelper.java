package com.github.sardul3.io.api_best_practices_boot.hateoas.config;

import com.github.sardul3.io.api_best_practices_boot.eTags.controllers.TransactionController;
import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;
import com.github.sardul3.io.api_best_practices_boot.pageFilterSort.controllers.TransactionsControllerPaged;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public class HATEOASHelper {

    public static <T> CollectionModel<EntityModel<T>> generatePaginatedLinks(Page<T> page, int currentPage, int pageSize) {
        // Convert each entity into an EntityModel and add self-links to each one
        CollectionModel<EntityModel<T>> model = CollectionModel.of(
                page.map(entity -> {
                    EntityModel<T> entityModel = EntityModel.of(entity);
                    // Add self link for each entity
                    entityModel.add(linkTo(methodOn(TransactionController.class)
                            .getTransaction(((Transaction) entity).getTransactionId(), null)).withSelfRel());
                    return entityModel;
                }).getContent());

        // Add next and previous page links if applicable
        if (page.hasNext()) {
            model.add(Link.of(linkToSelf(currentPage + 1, pageSize), "next"));
        }

        if (page.hasPrevious()) {
            model.add(Link.of(linkToSelf(currentPage - 1, pageSize), "prev"));
        }

        return model;
    }

    public static <T> EntityModel<T> generateEntityLinks(T entity, Long id) {
        // Create an entity model to wrap the entity
        EntityModel<T> model = EntityModel.of(entity);

        // Link to self (details)
        Link selfLink = linkTo(methodOn(TransactionController.class)
                .getTransaction(id, null))
                .withSelfRel();
        model.add(selfLink);

        // Add an edit link (make sure the editTransaction method exists)
        Link editLink = linkTo(methodOn(TransactionController.class)
                .updateTransactionStatus(id, null))
                .withRel("edit");
        model.add(editLink);

        return model;
    }

    // Helper method to generate pagination links with page and size parameters
    private static String linkToSelf(int page, int size) {
        return linkTo(methodOn(TransactionsControllerPaged.class)
                .getTransactions(null, null, null))
                .toUriComponentsBuilder()
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();
    }
}
