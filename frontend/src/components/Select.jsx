import React, { useState, useRef, useEffect, useCallback, useLayoutEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { createPortal } from "react-dom";

export default function Select({
  id,
  value,
  onChange,
  options = [],
  placeholder = "Select",
  disabled = false,
}) {
  const [open, setOpen] = useState(false);
  const [highlight, setHighlight] = useState(-1);
  const rootRef = useRef(null);
  const triggerRef = useRef(null);
  const [position, setPosition] = useState(null);
  const listId = id ? `${id}-listbox` : undefined;

  const normOptions = options.map((option) =>
    typeof option === "string"
      ? { value: option, label: option }
      : option
  );
  const selected = normOptions.find((opt) => opt.value === value);

  const close = useCallback(() => {
    setOpen(false);
    setHighlight(-1);
  }, []);

  useEffect(() => {
    if (!open) return;
    const handler = (event) => {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        close();
      }
    };
    window.addEventListener("mousedown", handler);
    window.addEventListener("touchstart", handler);
    return () => {
      window.removeEventListener("mousedown", handler);
      window.removeEventListener("touchstart", handler);
    };
  }, [open, close]);

  const onKeyDown = (event) => {
    if (disabled) return;
    switch (event.key) {
      case "ArrowDown":
        event.preventDefault();
        if (!open) {
          setOpen(true);
          setHighlight(0);
          return;
        }
        setHighlight((h) => Math.min(normOptions.length - 1, h < 0 ? 0 : h + 1));
        break;
      case "ArrowUp":
        event.preventDefault();
        if (!open) {
          setOpen(true);
          setHighlight(normOptions.length - 1);
          return;
        }
        setHighlight((h) => Math.max(0, h - 1));
        break;
      case "Home":
        if (open) {
          setHighlight(0);
          event.preventDefault();
        }
        break;
      case "End":
        if (open) {
          setHighlight(normOptions.length - 1);
          event.preventDefault();
        }
        break;
      case "Enter":
      case " ":
        if (!open) {
          setOpen(true);
          event.preventDefault();
          return;
        }
        if (highlight >= 0) {
          onChange(normOptions[highlight].value);
          close();
          event.preventDefault();
        }
        break;
      case "Escape":
        if (open) {
          close();
          event.preventDefault();
        }
        break;
      case "Tab":
        close();
        break;
      default:
        break;
    }
  };

  useEffect(() => {
    if (open) {
      const idx = normOptions.findIndex((opt) => opt.value === value);
      setHighlight(idx);
    }
  }, [open, value, normOptions]);

  useLayoutEffect(() => {
    if (open && triggerRef.current) {
      const rect = triggerRef.current.getBoundingClientRect();
      setPosition({
        top: rect.bottom + window.scrollY,
        left: rect.left + window.scrollX,
        width: rect.width,
      });
    }
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const handler = () => {
      if (triggerRef.current) {
        const rect = triggerRef.current.getBoundingClientRect();
        setPosition({
          top: rect.bottom + window.scrollY,
          left: rect.left + window.scrollX,
          width: rect.width,
        });
      }
    };
    window.addEventListener("resize", handler);
    window.addEventListener("scroll", handler, true);
    return () => {
      window.removeEventListener("resize", handler);
      window.removeEventListener("scroll", handler, true);
    };
  }, [open]);

  const popover =
    open &&
    position &&
    createPortal(
      <AnimatePresence>
        <motion.ul
          key="select-popover"
          id={listId}
          role="listbox"
          tabIndex={-1}
          initial={{ opacity: 0, y: 4, scale: 0.98 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: 4, scale: 0.98 }}
          transition={{ duration: 0.18, ease: [0.4, 0, 0.2, 1] }}
          className="select-popover"
          data-portal="true"
          style={{
            top: position.top,
            left: position.left,
            width: position.width,
            position: "absolute",
          }}
        >
          {normOptions.map((opt, index) => {
            const isSelected = opt.value === value;
            const isActive = index === highlight;
            return (
              <li
                key={opt.value}
                role="option"
                aria-selected={isSelected}
                className={`select-option ${isSelected ? "selected" : ""} ${
                  isActive ? "active" : ""
                }`}
                onMouseEnter={() => setHighlight(index)}
                onMouseDown={(event) => {
                  event.preventDefault();
                  onChange(opt.value);
                  close();
                }}
              >
                <span>{opt.label}</span>
                {isSelected && (
                  <span className="tick" aria-hidden="true">
                    ✓
                  </span>
                )}
              </li>
            );
          })}
        </motion.ul>
      </AnimatePresence>,
      document.body
    );

  return (
    <>
      <div ref={rootRef} className={`select-root ${disabled ? "is-disabled" : ""}`}>
        <button
          id={id}
          ref={triggerRef}
          type="button"
          className={`select-trigger ${open ? "open" : ""}`}
          aria-haspopup="listbox"
          aria-expanded={open}
          aria-controls={listId}
          disabled={disabled}
          onClick={() => !disabled && setOpen((o) => !o)}
          onKeyDown={onKeyDown}
        >
          <span className="select-value">
            {selected ? selected.label : <span className="placeholder">{placeholder}</span>}
          </span>
          <span className="select-caret" aria-hidden="true" />
        </button>
      </div>
      {popover}
    </>
  );
}
