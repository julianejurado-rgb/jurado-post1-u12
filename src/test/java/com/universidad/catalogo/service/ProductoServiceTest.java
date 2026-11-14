package com.universidad.catalogo.service;

import com.universidad.catalogo.model.Producto;
import com.universidad.catalogo.repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {
    @Mock    ProductoRepository repo;
    @InjectMocks ProductoService service;

    @Test void crear_conDatosValidos_guardaYRetorna() {
        Producto p = new Producto();
        p.setNombre("Teclado mecánico"); p.setPrecio(120.0); p.setCategoria("ELECTRONICA");
        when(repo.save(any())).thenReturn(p);
        assertThat(service.crear(p).getNombre()).isEqualTo("Teclado mecánico");
        verify(repo).save(p);
    }
    @Test void crear_conPrecioNegativo_lanzaIllegalArgumentException() {
        Producto p = new Producto();
        p.setNombre("Mouse"); p.setPrecio(-10.0);
        assertThrows(IllegalArgumentException.class, () -> service.crear(p));
        verify(repo, never()).save(any());
    }
    @Test void buscarPorId_noExiste_lanzaEntityNotFoundException() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.buscarPorId(99L));
    }
    @Test void actualizarPrecio_conValorValido_actualizaYGuarda() {
        Producto p = new Producto(); p.setId(1L); p.setPrecio(100.0);
        when(repo.findById(1L)).thenReturn(Optional.of(p));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.actualizarPrecio(1L, 150.0).getPrecio()).isEqualTo(150.0);
    }

    @Test void crear_conNombreVacio_lanzaIllegalArgumentException() {
        Producto p = new Producto();
        p.setNombre("  "); p.setPrecio(10.0);
        assertThrows(IllegalArgumentException.class, () -> service.crear(p));
        verify(repo, never()).save(any());
    }
    @Test void crear_conIdEnLaEntrada_ignoraElIdDelCliente() {
        Producto p = new Producto();
        p.setId(7L); p.setNombre("Monitor"); p.setPrecio(300.0);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.crear(p).getId()).isNull();
    }
    @Test void buscarPorId_existe_retornaElProducto() {
        Producto p = new Producto(); p.setId(5L); p.setNombre("Cuaderno");
        when(repo.findById(5L)).thenReturn(Optional.of(p));
        assertThat(service.buscarPorId(5L).getNombre()).isEqualTo("Cuaderno");
    }
    @Test void listarPorCategoria_conResultados_retornaLaLista() {
        Producto p = new Producto(); p.setNombre("Laptop"); p.setCategoria("ELECTRONICA");
        when(repo.findByCategoria("ELECTRONICA")).thenReturn(List.of(p));
        assertThat(service.listarPorCategoria("ELECTRONICA")).containsExactly(p);
    }
    @Test void actualizarPrecio_conPrecioInvalido_lanzaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.actualizarPrecio(1L, 0.0));
        verifyNoInteractions(repo);
    }
    @Test void actualizarPrecio_productoNoExiste_lanzaEntityNotFoundException() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.actualizarPrecio(99L, 50.0));
        verify(repo, never()).save(any());
    }
}
