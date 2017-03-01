package org.vaadin.addons;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.vaadin.addons.client.AutocompleteExtensionClientRpc;
import org.vaadin.addons.client.AutocompleteExtensionServerRpc;
import org.vaadin.addons.client.AutocompleteExtensionState;
import org.vaadin.addons.client.SuggestionData;

import com.vaadin.server.AbstractExtension;
import com.vaadin.ui.TextField;

/**
 * Extension for {@link TextField} for adding autocomplete functionality. As the
 * user types, suggestions appear under the text field component.
 * <p>
 * Suggestions are generated by a {@link SuggestionGenerator} and can be any
 * type. Suggestions are transferred to client as a list of caption-value pairs.
 * Caption is the visible HTML part. Value is to be set as text field value when
 * suggestion is selected. {@link SuggestionCaptionConverter} is responsible for
 * translating caption while {@link SuggestionValueConverter} translates the
 * value.
 *
 * @param <T>
 *         Type of the data to be generated. Can be the type of any Java
 *         object.
 * @see SuggestionGenerator
 * @see SuggestionCaptionConverter
 * @see SuggestionValueConverter
 */
public class AutocompleteExtension<T> extends AbstractExtension {

    private SuggestionGenerator<T> suggestionGenerator;
    private SuggestionCaptionConverter<T> captionConverter;
    private SuggestionValueConverter<T> valueConverter;

    private final SuggestionCaptionConverter<T> defaultCaptionConverter =
            (s, q) -> s.toString();
    private final SuggestionValueConverter<T> defaultValueConverter = T::toString;

    private AutocompleteExtensionServerRpc rpc = query -> {

        // TODO: 04/02/2017 Register RPC only when generator set
        Optional.ofNullable(suggestionGenerator).ifPresent(generator -> {
            // Generate suggestion list
            List<T> suggestions = generator
                    .apply(query, getState(false).suggestionListSize);

            // Get converters
            SuggestionCaptionConverter<T> cConverter = Optional
                    .ofNullable(captionConverter)
                    .orElse(defaultCaptionConverter);
            SuggestionValueConverter<T> vConverter = Optional
                    .ofNullable(valueConverter).orElse(defaultValueConverter);

            // Create a list of suggestion data and send it to the client
            getRpcProxy(AutocompleteExtensionClientRpc.class)
                    .showSuggestions(suggestions.stream()
                            .map(s -> new SuggestionData(vConverter.apply(s),
                                    cConverter.apply(s, query)))
                            .collect(Collectors.toList()), query);
        });
    };

    /**
     * Extends {@code textField} to add autocomplete functionality.
     *
     * @param textField
     *         Field to be extended.
     */
    public AutocompleteExtension(TextField textField) {
        registerRpc(rpc);

        super.extend(textField);
    }

    /**
     * Sets the suggestion generator for this extension. This generator is used
     * for creating suggestions when user types into the text field.
     *
     * @param suggestionGenerator
     *         Generator to be set.
     */
    public void setSuggestionGenerator(
            SuggestionGenerator<T> suggestionGenerator) {
        setSuggestionGenerator(suggestionGenerator, null, null);
    }

    /**
     * Sets the suggestion generator and converters for this extension.
     *
     * @param suggestionGenerator
     *         Generator for generating suggestions for user input.
     * @param valueConverter
     *         Converter to translate suggestion to value to be sat as text
     *         value when suggestion is selected.
     * @param captionConverter
     *         Converter to translate suggestion to safe HTML caption to be
     *         displayed for the user.
     */
    public void setSuggestionGenerator(
            SuggestionGenerator<T> suggestionGenerator,
            SuggestionValueConverter<T> valueConverter,
            SuggestionCaptionConverter<T> captionConverter) {
        this.suggestionGenerator = suggestionGenerator;
        this.valueConverter = valueConverter;
        this.captionConverter = captionConverter;
    }

    /**
     * Delay server callback for suggestions {@code delayMillis} milliseconds.
     *
     * @param delayMillis
     *         Delay in milliseconds. Must not be negative.
     */
    public void setSuggestionDelay(int delayMillis) {
        if (delayMillis < 0) {
            throw new IllegalArgumentException("Delay must be positive.");
        }

        if (!Objects.equals(getState(false).suggestionDelay, delayMillis)) {
            getState().suggestionDelay = delayMillis;
        }
    }

    /**
     * Returns delay for user input before server callback.
     *
     * @return Delay in milliseconds.
     */
    public int getSuggestionDelay() {
        return getState(false).suggestionDelay;
    }

    /**
     * Set maximum allowed size of the suggestion list.
     *
     * @param size
     *         Size of the suggestion list. Must not be negative.
     * @since 0.2.0
     */
    public void setSuggestionListSize(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Size must be positive");
        }

        if (!Objects.equals(getState(false).suggestionListSize, size)) {
            getState().suggestionListSize = size;
        }
    }

    /**
     * Returns the maximum allowed size of the suggestion list.
     *
     * @return Maximum allowed size of the suggestion list.
     * @since 0.2.0
     */
    public int getSuggestionListSize() {
        return getState(false).suggestionListSize;
    }

    @Override
    protected AutocompleteExtensionState getState() {
        return (AutocompleteExtensionState) super.getState();
    }

    @Override
    protected AutocompleteExtensionState getState(boolean markAsDirty) {
        return (AutocompleteExtensionState) super.getState(markAsDirty);
    }
}
